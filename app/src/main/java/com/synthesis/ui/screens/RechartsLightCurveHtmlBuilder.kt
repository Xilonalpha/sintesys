package com.synthesis.ui.screens

import com.synthesis.domain.model.exoplanet.FramePhotometrySample
import com.synthesis.domain.model.exoplanet.PeriodicTransitDetectionResult
import com.synthesis.domain.model.exoplanet.PhotometricDataPoint

/**
 * Generates an interactive astronomical HTML5/React/Recharts dashboard
 * specifically tailored for plotting FITS photometric time-series light curves
 * and highlighting detected periodic brightness drops (exoplanet transits).
 */
object RechartsLightCurveHtmlBuilder {

    fun buildHtmlFromSamples(
        samples: List<FramePhotometrySample>,
        periodicResult: PeriodicTransitDetectionResult?,
        targetName: String = "Sursă FITS CCD/CMOS"
    ): String {
        val points = samples.map { s ->
            PhotometricDataPoint(
                minuteOffset = s.timeMinutes,
                bjdTime = s.bjd,
                relativeFlux = s.normalizedFlux,
                fluxError = s.fluxError
            )
        }
        return buildHtml(points, periodicResult, targetName)
    }

    fun buildHtml(
        dataPoints: List<PhotometricDataPoint>,
        periodicResult: PeriodicTransitDetectionResult?,
        targetName: String = "Sursă FITS CCD/CMOS"
    ): String {
        val depthRatio = (periodicResult?.transitDepthFraction ?: 0.0).coerceAtLeast(0.0)
        val bestPeriod = periodicResult?.bestPeriodDays ?: 0.0
        val epochBjd = periodicResult?.epochBjd ?: (dataPoints.firstOrNull()?.bjdTime ?: 0.0)
        val halfDurationDays = (periodicResult?.transitDurationHours ?: 2.0) / 48.0

        // 1. Time Series Data JSON
        val rawTimeSeries = dataPoints.joinToString(prefix = "[", postfix = "]") { pt ->
            val time = Math.round(pt.minuteOffset * 100.0) / 100.0
            val bjd = Math.round(pt.bjdTime * 10000.0) / 10000.0
            val flux = Math.round(pt.relativeFlux * 10000.0) / 10000.0
            val error = Math.round(pt.fluxError * 100000.0) / 100000.0

            var isPeriodicDrop = false
            if (bestPeriod > 0.01) {
                val cycle = Math.round((pt.bjdTime - epochBjd) / bestPeriod).toDouble()
                val transitCenter = epochBjd + (cycle * bestPeriod)
                if (Math.abs(pt.bjdTime - transitCenter) <= halfDurationDays) {
                    isPeriodicDrop = true
                }
            } else if (pt.relativeFlux < 0.992) {
                isPeriodicDrop = true
            }

            val dropVal = if (isPeriodicDrop) "$flux" else "null"
            """{"time":$time,"bjd":$bjd,"flux":$flux,"error":$error,"isDrop":$isPeriodicDrop,"dropFlux":$dropVal}"""
        }

        // 2. Phase Folded Data JSON
        val rawPhaseFolded = periodicResult?.phaseFoldedPoints?.joinToString(prefix = "[", postfix = "]") { pt ->
            val ph = Math.round(pt.phase * 1000.0) / 1000.0
            val fl = Math.round(pt.relativeFlux * 10000.0) / 10000.0
            """{"phase":$ph,"flux":$fl,"error":${pt.fluxError},"isInTransit":${pt.isInTransit}}"""
        } ?: "[]"

        // 3. Binned Curve JSON
        val rawBinned = periodicResult?.binnedPhaseCurve?.joinToString(prefix = "[", postfix = "]") { b ->
            val ph = Math.round(b.phaseCenter * 1000.0) / 1000.0
            val fl = Math.round(b.meanFlux * 10000.0) / 10000.0
            """{"phase":$ph,"flux":$fl,"error":${b.fluxError}}"""
        } ?: "[]"

        // 4. Model Curve JSON
        val rawModel = periodicResult?.transitModelCurve?.joinToString(prefix = "[", postfix = "]") { (phase, flux) ->
            val ph = Math.round(phase * 1000.0) / 1000.0
            val fl = Math.round(flux * 10000.0) / 10000.0
            """{"phase":$ph,"modelFlux":$fl}"""
        } ?: "[]"

        // 5. Periodogram Data JSON
        val rawBls = periodicResult?.periodogram?.joinToString(prefix = "[", postfix = "]") { p ->
            val per = Math.round(p.periodDays * 1000.0) / 1000.0
            val pow = Math.round(p.power * 100.0) / 100.0
            """{"period":$per,"power":$pow,"depthPpm":${Math.round(p.depthPpm)}}"""
        } ?: "[]"

        val isDetected = periodicResult?.isDetected == true
        val periodVal = if (isDetected) String.format("%.3f", periodicResult!!.bestPeriodDays) else "--"
        val periodHours = if (isDetected) String.format("%.1f", periodicResult!!.bestPeriodHours) else "--"
        val depthVal = if (isDetected) String.format("%.0f", periodicResult!!.transitDepthPpm) else "--"
        val snrVal = if (isDetected) String.format("%.1f", periodicResult!!.transitSnr) else "--"
        val sdeVal = if (isDetected) String.format("%.2f", periodicResult!!.signalDetectionEfficiencySde) else "--"
        val radiusVal = if (isDetected) String.format("%.2f", periodicResult!!.physicalParameters.planetRadiusEarth) else "--"
        val classification = periodicResult?.physicalParameters?.planetClassification ?: "Sursă Fotometrică"

        return """
<!DOCTYPE html>
<html lang="ro">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>FITS Light Curve Recharts Visualization</title>
  
  <!-- React & Recharts CDN -->
  <script src="https://unpkg.com/react@18/umd/react.production.min.js" crossorigin></script>
  <script src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js" crossorigin></script>
  <script src="https://unpkg.com/prop-types@15.8.1/prop-types.min.js"></script>
  <script src="https://unpkg.com/recharts@2.12.7/umd/Recharts.min.js"></script>
  
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background-color: #070B14;
      color: #E2E8F0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", sans-serif;
      padding: 10px;
      overflow-x: hidden;
      user-select: none;
    }
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
      padding-bottom: 8px;
      border-bottom: 1px solid #1E293B;
    }
    .title-box {
      display: flex;
      flex-direction: column;
    }
    .main-title {
      font-size: 13px;
      font-weight: 700;
      color: #00E5FF;
      letter-spacing: 0.5px;
    }
    .sub-title {
      font-size: 10px;
      color: #94A3B8;
    }
    .status-pill {
      font-size: 10px;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 4px;
      background: ${if (isDetected) "rgba(0, 230, 118, 0.2)" else "rgba(100, 116, 139, 0.2)"};
      color: ${if (isDetected) "#00E676" else "#94A3B8"};
      border: 1px solid ${if (isDetected) "#00E676" else "#334155"};
    }
    .metrics-bar {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 6px;
      margin-bottom: 10px;
    }
    .metric-card {
      background: #0F172A;
      border: 1px solid #1E293B;
      border-radius: 6px;
      padding: 6px 8px;
      text-align: center;
    }
    .metric-label {
      font-size: 9px;
      color: #64748B;
      text-transform: uppercase;
      font-weight: 600;
    }
    .metric-val {
      font-size: 11px;
      font-weight: 700;
      color: #F8FAFC;
      font-family: monospace;
      margin-top: 2px;
    }
    .tab-bar {
      display: flex;
      gap: 6px;
      margin-bottom: 10px;
    }
    .tab-btn {
      flex: 1;
      background: #0F172A;
      border: 1px solid #1E293B;
      color: #94A3B8;
      font-size: 11px;
      font-weight: 600;
      padding: 6px 0;
      border-radius: 6px;
      cursor: pointer;
      text-align: center;
      transition: all 0.2s;
    }
    .tab-btn.active {
      background: rgba(0, 229, 255, 0.15);
      border-color: #00E5FF;
      color: #00E5FF;
    }
    .chart-container {
      background: #0B1120;
      border: 1px solid #1E293B;
      border-radius: 8px;
      padding: 8px 4px 4px 4px;
      position: relative;
    }
    .legend-row {
      display: flex;
      justify-content: center;
      gap: 12px;
      margin-top: 6px;
      font-size: 10px;
      color: #94A3B8;
    }
    .legend-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .legend-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }
    .drop-banner {
      background: rgba(0, 230, 118, 0.1);
      border: 1px solid rgba(0, 230, 118, 0.3);
      border-radius: 6px;
      padding: 8px;
      margin-top: 8px;
      font-size: 11px;
      color: #E2E8F0;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .custom-tooltip {
      background: #0F172A;
      border: 1px solid #00E5FF;
      border-radius: 6px;
      padding: 6px 10px;
      font-size: 10px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.5);
    }
  </style>
</head>
<body>
  <div id="root"></div>

  <script>
    const { useState } = React;
    const {
      ResponsiveContainer,
      ComposedChart,
      ScatterChart,
      Scatter,
      Line,
      XAxis,
      YAxis,
      CartesianGrid,
      Tooltip,
      ReferenceLine,
      ReferenceArea,
      Brush
    } = window.Recharts;

    const timeSeriesData = $rawTimeSeries;
    const phaseFoldedData = $rawPhaseFolded;
    const binnedData = $rawBinned;
    const modelData = $rawModel;
    const blsData = $rawBls;

    function CustomTooltip({ active, payload }) {
      if (active && payload && payload.length) {
        const d = payload[0].payload;
        return React.createElement("div", { className: "custom-tooltip" },
          d.time !== undefined && React.createElement("div", { style: { color: "#94A3B8" } }, "T = " + d.time + " min | BJD " + d.bjd),
          d.phase !== undefined && React.createElement("div", { style: { color: "#94A3B8" } }, "Fază = " + d.phase),
          React.createElement("div", { style: { color: "#00E5FF", fontWeight: "bold" } }, "Flux = " + d.flux),
          d.isDrop && React.createElement("div", { style: { color: "#00E676", fontWeight: "bold", marginTop: 2 } }, "★ TRANZIT PERIODIC IDENTIFICAT"),
          d.power !== undefined && React.createElement("div", { style: { color: "#FFB300" } }, "Putere BLS = " + d.power)
        );
      }
      return null;
    }

    function LightCurveDashboard() {
      const [activeTab, setActiveTab] = useState("time"); // 'time', 'phase', 'bls'

      return React.createElement("div", null,
        // Header
        React.createElement("div", { className: "header" },
          React.createElement("div", { className: "title-box" },
            React.createElement("span", { className: "main-title" }, "GRAFIC RECHARTS: FOTOMETRIE FITS"),
            React.createElement("span", { className: "sub-title" }, "$targetName | Filtru SNR Pre-Procesare Activ")
          ),
          React.createElement("span", { className: "status-pill" },
            $isDetected ? "★ TRANZIT DETECTAT" : "MONITORIZARE"
          )
        ),

        // Metrics Grid
        React.createElement("div", { className: "metrics-bar" },
          React.createElement("div", { className: "metric-card" },
            React.createElement("div", { className: "metric-label" }, "Perioadă"),
            React.createElement("div", { className: "metric-val", style: { color: "#00E676" } }, "$periodVal d")
          ),
          React.createElement("div", { className: "metric-card" },
            React.createElement("div", { className: "metric-label" }, "Adâncime"),
            React.createElement("div", { className: "metric-val", style: { color: "#00E5FF" } }, "$depthVal ppm")
          ),
          React.createElement("div", { className: "metric-card" },
            React.createElement("div", { className: "metric-label" }, "Rază Planetă"),
            React.createElement("div", { className: "metric-val", style: { color: "#FFB300" } }, "$radiusVal R⊕")
          ),
          React.createElement("div", { className: "metric-card" },
            React.createElement("div", { className: "metric-label" }, "BLS SDE"),
            React.createElement("div", { className: "metric-val", style: { color: "#F8FAFC" } }, "$sdeVal σ")
          )
        ),

        // Tab Selector
        React.createElement("div", { className: "tab-bar" },
          React.createElement("div", {
            className: "tab-btn " + (activeTab === "time" ? "active" : ""),
            onClick: () => setActiveTab("time")
          }, "⏱️ Serie Temporală (Min)"),
          React.createElement("div", {
            className: "tab-btn " + (activeTab === "phase" ? "active" : ""),
            onClick: () => setActiveTab("phase")
          }, "🪐 Curbă Pliată (Fază)"),
          React.createElement("div", {
            className: "tab-btn " + (activeTab === "bls" ? "active" : ""),
            onClick: () => setActiveTab("bls")
          }, "📈 Spectru BLS")
        ),

        // Chart Container
        React.createElement("div", { className: "chart-container" },
          activeTab === "time" && React.createElement(ResponsiveContainer, { width: "100%", height: 210 },
            React.createElement(ComposedChart, { data: timeSeriesData, margin: { top: 10, right: 10, left: -20, bottom: 5 } },
              React.createElement(CartesianGrid, { stroke: "#1E293B", strokeDasharray: "2 2" }),
              React.createElement(XAxis, { dataKey: "time", stroke: "#64748B", fontSize: 10, unit: "m" }),
              React.createElement(YAxis, { domain: ["auto", "auto"], stroke: "#64748B", fontSize: 10 }),
              React.createElement(Tooltip, { content: React.createElement(CustomTooltip) }),
              React.createElement(ReferenceLine, { y: 1.0, stroke: "#64748B", strokeDasharray: "3 3" }),
              ${if (depthRatio > 0.0) "React.createElement(ReferenceLine, { y: (1.0 - $depthRatio).toFixed(4), stroke: '#00E676', strokeDasharray: '4 4' })," else ""}
              // Baseline out-of-transit line
              React.createElement(Line, {
                type: "monotone",
                dataKey: "flux",
                stroke: "rgba(0, 229, 255, 0.4)",
                strokeWidth: 1,
                dot: { r: 2, fill: "#00E5FF" },
                isAnimationActive: false
              }),
              // Highlighted periodic transit drops in green
              React.createElement(Line, {
                type: "monotone",
                dataKey: "dropFlux",
                stroke: "#00E676",
                strokeWidth: 2,
                dot: { r: 4, fill: "#00E676", stroke: "#FFFFFF", strokeWidth: 1 },
                connectNulls: false,
                isAnimationActive: false
              }),
              React.createElement(Brush, { dataKey: "time", height: 20, stroke: "#00E5FF", fill: "#0A0E17", travellerWidth: 10 })
            )
          ),

          activeTab === "phase" && React.createElement(ResponsiveContainer, { width: "100%", height: 210 },
            React.createElement(ComposedChart, { margin: { top: 10, right: 10, left: -20, bottom: 5 } },
              React.createElement(CartesianGrid, { stroke: "#1E293B", strokeDasharray: "2 2" }),
              React.createElement(XAxis, { dataKey: "phase", type: "number", domain: [-0.5, 0.5], stroke: "#64748B", fontSize: 10 }),
              React.createElement(YAxis, { dataKey: "flux", domain: ["auto", "auto"], stroke: "#64748B", fontSize: 10 }),
              React.createElement(Tooltip, { content: React.createElement(CustomTooltip) }),
              React.createElement(ReferenceLine, { x: 0, stroke: "#00E5FF", strokeDasharray: "3 3" }),
              React.createElement(ReferenceLine, { y: 1.0, stroke: "#64748B", strokeDasharray: "2 2" }),
              // Phase observations
              React.createElement(Scatter, {
                data: phaseFoldedData,
                dataKey: "flux",
                fill: "#38BDF8",
                opacity: 0.5,
                shape: "circle"
              }),
              // Binned points
              React.createElement(Line, {
                data: binnedData,
                type: "monotone",
                dataKey: "flux",
                stroke: "#FFB300",
                strokeWidth: 2,
                dot: { r: 3, fill: "#FFB300" },
                isAnimationActive: false
              }),
              // Transit Model curve
              React.createElement(Line, {
                data: modelData,
                type: "monotone",
                dataKey: "modelFlux",
                stroke: "#00E676",
                strokeWidth: 2.5,
                dot: false,
                isAnimationActive: false
              })
            )
          ),

          activeTab === "bls" && React.createElement(ResponsiveContainer, { width: "100%", height: 210 },
            React.createElement(ComposedChart, { data: blsData, margin: { top: 10, right: 10, left: -20, bottom: 5 } },
              React.createElement(CartesianGrid, { stroke: "#1E293B", strokeDasharray: "2 2" }),
              React.createElement(XAxis, { dataKey: "period", stroke: "#64748B", fontSize: 10, unit: "d" }),
              React.createElement(YAxis, { stroke: "#64748B", fontSize: 10 }),
              React.createElement(Tooltip, { content: React.createElement(CustomTooltip) }),
              ${if (isDetected) "React.createElement(ReferenceLine, { x: $bestPeriod, stroke: '#00E676', strokeWidth: 2, strokeDasharray: '3 3' })," else ""}
              React.createElement(Line, {
                type: "monotone",
                dataKey: "power",
                stroke: "#00E5FF",
                strokeWidth: 2,
                dot: false,
                isAnimationActive: false
              })
            )
          )
        ),

        // Legend
        React.createElement("div", { className: "legend-row" },
          activeTab === "time" && [
            React.createElement("div", { className: "legend-item", key: "1" },
              React.createElement("div", { className: "legend-dot", style: { background: "#00E5FF" } }),
              React.createElement("span", null, "Linie de Bază")
            ),
            React.createElement("div", { className: "legend-item", key: "2" },
              React.createElement("div", { className: "legend-dot", style: { background: "#00E676" } }),
              React.createElement("span", null, "Scădere Periodică (Tranzit)")
            )
          ],
          activeTab === "phase" && [
            React.createElement("div", { className: "legend-item", key: "3" },
              React.createElement("div", { className: "legend-dot", style: { background: "#38BDF8" } }),
              React.createElement("span", null, "Puncte Faze")
            ),
            React.createElement("div", { className: "legend-item", key: "4" },
              React.createElement("div", { className: "legend-dot", style: { background: "#FFB300" } }),
              React.createElement("span", null, "Medie Binned")
            ),
            React.createElement("div", { className: "legend-item", key: "5" },
              React.createElement("div", { className: "legend-dot", style: { background: "#00E676" } }),
              React.createElement("span", null, "Model Teoretic")
            )
          ],
          activeTab === "bls" && [
            React.createElement("div", { className: "legend-item", key: "6" },
              React.createElement("div", { className: "legend-dot", style: { background: "#00E5FF" } }),
              React.createElement("span", null, "Spectru Putere BLS")
            ),
            React.createElement("div", { className: "legend-item", key: "7" },
              React.createElement("div", { className: "legend-dot", style: { background: "#00E676" } }),
              React.createElement("span", null, "Vârf Perioadă Confirmată")
            )
          ]
        ),

        // Highlight Info Banner
        $isDetected ? React.createElement("div", { className: "drop-banner" },
          React.createElement("span", null, "★ Scăderi periodice evidențiate la P = $periodVal zile ($periodHours ore), adâncime $depthVal ppm."),
          React.createElement("span", { style: { color: "#00E676", fontWeight: "bold" } }, "$classification")
        ) : null
      );
    }

    ReactDOM.render(React.createElement(LightCurveDashboard), document.getElementById("root"));
  </script>
</body>
</html>
""".trimIndent()
    }
}
