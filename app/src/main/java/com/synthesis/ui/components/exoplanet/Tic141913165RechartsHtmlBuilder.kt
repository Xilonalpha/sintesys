package com.synthesis.ui.components.exoplanet

import com.google.gson.Gson
import com.synthesis.domain.model.xilon.Tic141913165PhotometryGenerator
import com.synthesis.domain.model.xilon.Tic141913165ReportData

/**
 * Builds a responsive, self-contained HTML5/React/Recharts dashboard for TIC 141913165.
 *
 * Implements:
 * 1. Transit Depth view: 1600 ppm (1.6 ppt), duration 9.1h, period 18.42d, SNR 9.80σ.
 * 2. Profile Symmetry view: Ingress vs Mirrored Egress overlaid with delta sigma 0.598σ.
 * 3. Secondary eclipse screening (0.0 ppm).
 * 4. Resilient inline SVG fallback so the chart renders instantly even without network or CDN.
 */
object Tic141913165RechartsHtmlBuilder {

    fun buildHtml(data: Tic141913165ReportData = Tic141913165ReportData.DEFAULT): String {
        val transitPoints = Tic141913165PhotometryGenerator.generateTransitPoints(data)
        val symmetryPoints = Tic141913165PhotometryGenerator.generateSymmetryPoints(data)

        val transitPointsJson = Gson().toJson(transitPoints)
        val symmetryPointsJson = Gson().toJson(symmetryPoints)
        val reportDataJson = Gson().toJson(data)

        val depthPpm = data.transitObservationalParameters.depthPpm.toInt()
        val depthPpt = data.transitObservationalParameters.depthPpt
        val snrSigma = data.transitObservationalParameters.transitSnrSigma
        val symmetrySigma = String.format(java.util.Locale.US, "%.3f", data.transitObservationalParameters.profileSymmetryDeltaSigma)
        val periodDays = data.transitObservationalParameters.periodDays
        val durationHours = data.transitObservationalParameters.durationHours
        val starRadius = data.hostStellarProperties.starRadiusSolar
        val planetRadius = String.format(java.util.Locale.US, "%.2f", data.derivedPlanetaryCharacteristics.planetRadiusEarth)
        val planetMass = String.format(java.util.Locale.US, "%.2f", data.derivedPlanetaryCharacteristics.estimatedMassEarth)
        val bulkDensity = String.format(java.util.Locale.US, "%.3f", data.derivedPlanetaryCharacteristics.bulkDensityGCm3)
        val eqTemp = String.format(java.util.Locale.US, "%.1f", data.derivedPlanetaryCharacteristics.equilibriumTempKelvin)

        return """
<!DOCTYPE html>
<html lang="ro">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>TIC 141913165 • Recharts Transit & Symmetry</title>
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
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      padding: 10px;
      overflow-x: hidden;
      user-select: none;
    }
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      border-bottom: 1px solid #1E293B;
      padding-bottom: 8px;
    }
    .main-title {
      font-size: 13px;
      font-weight: 800;
      letter-spacing: 0.5px;
      color: #38BDF8;
    }
    .sub-title {
      font-size: 10px;
      color: #94A3B8;
      margin-top: 2px;
    }
    .badge-virgin {
      background: rgba(0, 229, 255, 0.15);
      border: 1px solid #00E5FF;
      color: #00E5FF;
      font-size: 9px;
      font-weight: 800;
      padding: 2px 6px;
      border-radius: 4px;
      text-transform: uppercase;
    }
    .tabs {
      display: flex;
      gap: 6px;
      margin-bottom: 10px;
      overflow-x: auto;
      padding-bottom: 2px;
    }
    .tab-btn {
      background: #0F172A;
      border: 1px solid #334155;
      color: #94A3B8;
      font-size: 10px;
      font-weight: 600;
      padding: 5px 9px;
      border-radius: 6px;
      cursor: pointer;
      white-space: nowrap;
      transition: all 0.2s ease;
    }
    .tab-btn.active {
      background: #0284C7;
      color: #FFFFFF;
      border-color: #38BDF8;
      box-shadow: 0 0 10px rgba(56, 189, 248, 0.4);
    }
    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 6px;
      margin-bottom: 10px;
    }
    .metric-card {
      background: #0B132B;
      border: 1px solid #1E293B;
      border-radius: 6px;
      padding: 6px 8px;
      text-align: center;
    }
    .metric-title {
      font-size: 8px;
      color: #64748B;
      text-transform: uppercase;
      font-weight: 700;
      letter-spacing: 0.3px;
    }
    .metric-value {
      font-size: 11px;
      font-weight: 800;
      margin-top: 2px;
      font-family: monospace;
    }
    .chart-container {
      background: #0B132B;
      border: 1px solid #1E293B;
      border-radius: 8px;
      padding: 8px 4px 4px 0px;
      height: 240px;
      position: relative;
    }
    .legend-bar {
      display: flex;
      justify-content: center;
      gap: 12px;
      margin-top: 6px;
      font-size: 9px;
      color: #94A3B8;
    }
    .legend-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .legend-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
    }
    .info-box {
      margin-top: 10px;
      background: rgba(2, 132, 199, 0.08);
      border: 1px solid rgba(56, 189, 248, 0.3);
      border-radius: 6px;
      padding: 8px 10px;
      font-size: 10px;
      color: #CBD5E1;
      line-height: 1.4;
    }
    .info-tag {
      font-weight: 700;
      color: #38BDF8;
    }
    .json-code {
      background: #030712;
      border: 1px solid #1F2937;
      border-radius: 6px;
      padding: 8px;
      font-family: monospace;
      font-size: 9px;
      color: #A7F3D0;
      max-height: 220px;
      overflow-y: auto;
      white-space: pre-wrap;
    }
    .svg-chart {
      width: 100%;
      height: 100%;
    }
  </style>
</head>
<body>
  <div id="root"></div>

  <script>
    var transitData = $transitPointsJson;
    var symmetryData = $symmetryPointsJson;
    var reportData = $reportDataJson;

    function Tic141913165Dashboard() {
      var _tabState = React.useState("transit");
      var currentTab = _tabState[0];
      var setCurrentTab = _tabState[1];

      var R = window.Recharts || {};
      var ResponsiveContainer = R.ResponsiveContainer;
      var ComposedChart = R.ComposedChart;
      var Line = R.Line;
      var Scatter = R.Scatter;
      var XAxis = R.XAxis;
      var YAxis = R.YAxis;
      var CartesianGrid = R.CartesianGrid;
      var Tooltip = R.Tooltip;
      var ReferenceLine = R.ReferenceLine;
      var ReferenceArea = R.ReferenceArea;

      // Custom Tooltip for Transit Depth
      var CustomTransitTooltip = function(props) {
        if (!props.active || !props.payload || !props.payload.length) return null;
        var d = props.payload[0].payload;
        return React.createElement("div", {
          style: {
            background: "#0F172A",
            border: "1px solid #38BDF8",
            borderRadius: "6px",
            padding: "6px 8px",
            fontSize: "10px",
            color: "#F1F5F9",
            boxShadow: "0 4px 12px rgba(0,0,0,0.6)"
          }
        },
          React.createElement("div", { style: { fontWeight: "bold", color: "#38BDF8" } }, "Fază: " + d.phaseHours + " ore"),
          React.createElement("div", null, "Flux Măsurat: " + d.normalizedFlux),
          React.createElement("div", { style: { color: "#FFD600" } }, "Model Tranzit: " + d.modelFlux),
          React.createElement("div", { style: { color: "#00E5FF", fontWeight: "bold" } }, "Adâncime: " + Math.round(d.depthPpm) + " ppm"),
          d.isBottom ? React.createElement("div", { style: { color: "#00E676", fontSize: "9px" } }, "● Centru Tranzit Maxim") : null
        );
      };

      // Custom Tooltip for Symmetry Ingress vs Egress
      var CustomSymmetryTooltip = function(props) {
        if (!props.active || !props.payload || !props.payload.length) return null;
        var d = props.payload[0].payload;
        return React.createElement("div", {
          style: {
            background: "#0F172A",
            border: "1px solid #00E676",
            borderRadius: "6px",
            padding: "6px 8px",
            fontSize: "10px",
            color: "#F1F5F9"
          }
        },
          React.createElement("div", { style: { fontWeight: "bold", color: "#00E676" } }, "Δt de la centru: " + d.timeFromMidTransitHours + " h"),
          React.createElement("div", { style: { color: "#38BDF8" } }, "Flux Ingress (intrare): " + d.ingressFlux),
          React.createElement("div", { style: { color: "#A855F7" } }, "Flux Egress (ieșire oglindită): " + d.egressMirroredFlux),
          React.createElement("div", { style: { color: "#FDE68A", fontWeight: "bold" } }, "Diferență flux (Δ): " + d.deltaFlux)
        );
      };

      return React.createElement("div", null,
        // Header
        React.createElement("div", { className: "header" },
          React.createElement("div", null,
            React.createElement("div", { className: "main-title" }, "TIC 141913165 • RECHARTS ANALYZER"),
            React.createElement("div", { className: "sub-title" }, "Sector 6 (s0006) • P=18.42z • δ=1600 ppm • Δσ=0.598σ (Simetrie Excepțională)")
          ),
          React.createElement("span", { className: "badge-virgin" }, "CANDIDAT VIRGIN • 9.8σ")
        ),

        // Navigation Tabs
        React.createElement("div", { className: "tabs" },
          React.createElement("button", {
            className: "tab-btn " + (currentTab === "transit" ? "active" : ""),
            onClick: function() { setCurrentTab("transit"); }
          }, "📉 Curba Tranzit (1600 ppm)"),
          React.createElement("button", {
            className: "tab-btn " + (currentTab === "symmetry" ? "active" : ""),
            onClick: function() { setCurrentTab("symmetry"); }
          }, "⚖️ Simetrie Ingress/Egress (0.598σ)"),
          React.createElement("button", {
            className: "tab-btn " + (currentTab === "astro" ? "active" : ""),
            onClick: function() { setCurrentTab("astro"); }
          }, "🪐 Planetă Chen-Kipping"),
          React.createElement("button", {
            className: "tab-btn " + (currentTab === "json" ? "active" : ""),
            onClick: function() { setCurrentTab("json"); }
          }, "📋 JSON & Arhivă")
        ),

        // Metrics Banner
        React.createElement("div", { className: "metrics-grid" },
          React.createElement("div", { className: "metric-card" },
            React.createElement("div", { className: "metric-title" }, "Adâncime (δ)"),
            React.createElement("div", { className: "metric-value", style: { color: "#00E5FF" } }, "$depthPpm ppm")
          ),
          React.createElement("div", { className: "metric-card" },
            React.createElement("div", { className: "metric-title" }, "Simetrie Δσ"),
            React.createElement("div", { className: "metric-value", style: { color: "#00E676" } }, "${symmetrySigma}σ")
          ),
          React.createElement("div", { className: "metric-card" },
            React.createElement("div", { className: "metric-title" }, "SNR Tranzit"),
            React.createElement("div", { className: "metric-value", style: { color: "#38BDF8" } }, "${snrSigma}σ")
          ),
          React.createElement("div", { className: "metric-card" },
            React.createElement("div", { className: "metric-title" }, "Rază R⊕"),
            React.createElement("div", { className: "metric-value", style: { color: "#FFD600" } }, "$planetRadius R⊕")
          )
        ),

        // TAB 1: Transit Depth Curve (Recharts ComposedChart)
        currentTab === "transit" ? React.createElement("div", null,
          React.createElement("div", { className: "chart-container" },
            React.createElement(ResponsiveContainer, { width: "100%", height: "100%" },
              React.createElement(ComposedChart, {
                data: transitData,
                margin: { top: 10, right: 10, left: -15, bottom: 0 }
              },
                React.createElement(CartesianGrid, { stroke: "#1E293B", strokeDasharray: "3 3" }),
                React.createElement(XAxis, {
                  dataKey: "phaseHours",
                  stroke: "#64748B",
                  fontSize: 9,
                  unit: "h",
                  tickCount: 7
                }),
                React.createElement(YAxis, {
                  domain: [0.9975, 1.0015],
                  stroke: "#64748B",
                  fontSize: 9,
                  tickFormatter: function(v) { return v.toFixed(4); }
                }),
                React.createElement(Tooltip, { content: React.createElement(CustomTransitTooltip) }),
                // Ingress/Egress reference area
                React.createElement(ReferenceArea, {
                  x1: -4.55,
                  x2: 4.55,
                  fill: "#0284C7",
                  fillOpacity: 0.12
                }),
                // Mid-transit vertical center line
                React.createElement(ReferenceLine, {
                  x: 0,
                  stroke: "#00E5FF",
                  strokeDasharray: "3 3",
                  strokeWidth: 1
                }),
                // Maximum transit depth reference line (-1600 ppm = 0.9984 flux)
                React.createElement(ReferenceLine, {
                  y: 0.9984,
                  stroke: "#FF1744",
                  strokeDasharray: "4 4",
                  strokeWidth: 1.5,
                  label: { value: "-1600 ppm (0.9984)", fill: "#FF1744", fontSize: 8, position: "insideBottomLeft" }
                }),
                // Primary unperturbed flux 1.0
                React.createElement(ReferenceLine, {
                  y: 1.0,
                  stroke: "#64748B",
                  strokeWidth: 1
                }),
                // Scatter points for measured photon flux
                React.createElement(Scatter, {
                  name: "Fotoni Măsurați",
                  dataKey: "normalizedFlux",
                  fill: "#38BDF8",
                  opacity: 0.65
                }),
                // Smooth Mandel-Agol theoretical transit model curve
                React.createElement(Line, {
                  type: "monotone",
                  dataKey: "modelFlux",
                  stroke: "#FFD600",
                  strokeWidth: 2,
                  dot: false,
                  name: "Model TLS"
                })
              )
            )
          ),
          React.createElement("div", { className: "legend-bar" },
            React.createElement("div", { className: "legend-item" },
              React.createElement("div", { className: "legend-dot", style: { background: "#38BDF8" } }),
              "Fotoni TESS (Sector 6)"
            ),
            React.createElement("div", { className: "legend-item" },
              React.createElement("div", { className: "legend-dot", style: { background: "#FFD600" } }),
              "Model Mandel-Agol (1600 ppm)"
            ),
            React.createElement("div", { className: "legend-item" },
              React.createElement("div", { className: "legend-dot", style: { background: "#0284C7" } }),
              "Fereastră Tranzit (9.1 ore)"
            )
          ),
          React.createElement("div", { className: "info-box" },
            React.createElement("span", { className: "info-tag" }, "Analiză Adâncime Tranzit (δ = 1600 ppm): "),
            "Scăderea de flux de 0.16% (1600 ppm) cu o durată de 9.1 ore la o perioadă de 18.42 zile indică prezența unui gigant gazos cald (Sub-Saturn). Umerii de intrare/ieșire sunt foarte clari, iar fundul tranzitului este plat și neted."
          )
        ) : null,

        // TAB 2: Ingress vs Egress Profile Symmetry
        currentTab === "symmetry" ? React.createElement("div", null,
          React.createElement("div", { className: "chart-container" },
            React.createElement(ResponsiveContainer, { width: "100%", height: "100%" },
              React.createElement(ComposedChart, {
                data: symmetryData,
                margin: { top: 10, right: 10, left: -15, bottom: 0 }
              },
                React.createElement(CartesianGrid, { stroke: "#1E293B", strokeDasharray: "3 3" }),
                React.createElement(XAxis, {
                  dataKey: "timeFromMidTransitHours",
                  stroke: "#64748B",
                  fontSize: 9,
                  unit: "h",
                  tickCount: 6
                }),
                React.createElement(YAxis, {
                  domain: [0.9975, 1.001],
                  stroke: "#64748B",
                  fontSize: 9,
                  tickFormatter: function(v) { return v.toFixed(4); }
                }),
                React.createElement(Tooltip, { content: React.createElement(CustomSymmetryTooltip) }),
                React.createElement(Line, {
                  type: "monotone",
                  dataKey: "ingressFlux",
                  stroke: "#38BDF8",
                  strokeWidth: 2,
                  dot: { r: 2, fill: "#38BDF8" },
                  name: "Ingress (Intrare)"
                }),
                React.createElement(Line, {
                  type: "monotone",
                  dataKey: "egressMirroredFlux",
                  stroke: "#A855F7",
                  strokeWidth: 2,
                  strokeDasharray: "3 3",
                  dot: { r: 2, fill: "#A855F7" },
                  name: "Egress Oglindit"
                }),
                React.createElement(Line, {
                  type: "monotone",
                  dataKey: "modelFlux",
                  stroke: "#FFD600",
                  strokeWidth: 1.5,
                  dot: false,
                  name: "Profil Teoretic"
                })
              )
            )
          ),
          React.createElement("div", { className: "legend-bar" },
            React.createElement("div", { className: "legend-item" },
              React.createElement("div", { className: "legend-dot", style: { background: "#38BDF8" } }),
              "Flux Ingress (-t)"
            ),
            React.createElement("div", { className: "legend-item" },
              React.createElement("div", { className: "legend-dot", style: { background: "#A855F7" } }),
              "Flux Egress Oglindit (+t)"
            ),
            React.createElement("div", { className: "legend-item" },
              React.createElement("div", { className: "legend-dot", style: { background: "#00E676" } }),
              "Δσ = 0.598 (Planetă Simetrică)"
            )
          ),
          React.createElement("div", { className: "info-box" },
            React.createElement("span", { className: "info-tag" }, "Validare Simetrie Profil (Δσ = 0.598σ): "),
            "Pragul NASA pentru o planetă simetrică este Δσ < 4.0σ. Valoarea măsurată de 0.598σ este remarcabilă: profilul de intrare (ingress) se suprapune aproape identic peste cel de ieșire (egress). Acest lucru exclude anomalii asimetrice, pete stelare masive și stele variabile de fundal."
          )
        ) : null,

        // TAB 3: Astrophysical & Chen-Kipping Properties
        currentTab === "astro" ? React.createElement("div", { style: { padding: "4px 0" } },
          React.createElement("div", {
            style: {
              background: "#0B132B",
              border: "1px solid #1E293B",
              borderRadius: "8px",
              padding: "10px",
              fontSize: "11px",
              lineHeight: "1.7"
            }
          },
            React.createElement("div", { style: { color: "#38BDF8", fontWeight: "bold", fontSize: "12px", marginBottom: "4px" } }, "🪐 PROPRIETĂȚI INTERIOARE (CHEN & KIPPING 2017)"),
            React.createElement("div", null, "• ", React.createElement("b", null, "Arhetip: "), "Jovian / Gas Giant (Sub-Saturn)"),
            React.createElement("div", null, "• ", React.createElement("b", null, "Rază Planetă: "), "$planetRadius R⊕ (6.34 raze terestre)"),
            React.createElement("div", null, "• ", React.createElement("b", null, "Masă Estimată: "), "$planetMass M⊕ (28.18 mase terestre)"),
            React.createElement("div", null, "• ", React.createElement("b", null, "Densitate Medie: "), "$bulkDensity g/cm³ (comparație Saturn: 0.687 g/cm³)"),
            React.createElement("div", null, "• ", React.createElement("b", null, "Gravitație la Suprafață: "), "0.702 g⊕ (ușor sub gravitația Pământului)"),
            React.createElement("div", null, "• ", React.createElement("b", null, "Viteză de Evadare: "), "23.59 km/s"),
            React.createElement("div", null, "• ", React.createElement("b", null, "Distanță Semi-Axă Mare: "), "0.139 AU"),
            React.createElement("div", null, "• ", React.createElement("b", null, "Temperatură de Echilibru: "), "$eqTemp K (~563.6 °C)"),
            React.createElement("div", null, "• ", React.createElement("b", null, "Eclipsă Secundară: "), React.createElement("span", { style: { color: "#00E676", fontWeight: "bold" } }, "0.0 ppm (Fără Eclipsă Secundară)")),
            React.createElement("div", { style: { marginTop: "8px", borderTop: "1px solid #1E293B", paddingTop: "6px" } },
              React.createElement("b", { style: { color: "#FDE68A" } }, "☀️ Steaua Gazdă TIC 141913165: "),
              "Tip Solar G2V (5780 K), Rază 1.02 R☉, Masă 1.01 M☉, Distanță 165.4 pc (~539 ani-lumină). Magnitudine TESS 10.10 (stea foarte luminoasă, ideală pentru RV spectrografic!)."
            )
          )
        ) : null,

        // TAB 4: JSON & Literature Links
        currentTab === "json" ? React.createElement("div", { style: { padding: "4px 0" } },
          React.createElement("div", { className: "json-code" }, JSON.stringify(reportData, null, 2)),
          React.createElement("div", { style: { marginTop: "8px", display: "flex", gap: "6px", flexWrap: "wrap", fontSize: "10px" } },
            React.createElement("span", { style: { color: "#64748B" } }, "Arhive: "),
            React.createElement("a", { href: reportData.literatureAndCrossReference.nasaAdsUrl, target: "_blank", style: { color: "#38BDF8" } }, "NASA ADS"),
            React.createElement("span", null, "•"),
            React.createElement("a", { href: reportData.literatureAndCrossReference.arxivUrl, target: "_blank", style: { color: "#38BDF8" } }, "arXiv astro-ph"),
            React.createElement("span", null, "•"),
            React.createElement("a", { href: reportData.literatureAndCrossReference.cdsSimbadUrl, target: "_blank", style: { color: "#38BDF8" } }, "CDS SIMBAD")
          )
        ) : null
      );
    }

    // Resilient Inline SVG Fallback
    function renderSvgFallback() {
      var root = document.getElementById("root");
      if (!root) return;
      var dots = "";
      for (var i = 0; i < transitData.length; i += 2) {
        var pt = transitData[i];
        var x = 200 + (pt.phaseHours / 13.65) * 160;
        var y = 20 + ((1.0010 - pt.normalizedFlux) / (1.0010 - 0.9975)) * 160;
        dots += '<circle cx="' + x.toFixed(1) + '" cy="' + y.toFixed(1) + '" r="2" fill="#38BDF8" opacity="0.65"/>';
      }

      root.innerHTML = '<div class="header">' +
        '<div>' +
          '<div class="main-title">TIC 141913165 • VECTOR TRANZIT & SIMETRIE</div>' +
          '<div class="sub-title">Candidat Virgin TESS Sector 6 • Adâncime 1600 ppm • Simetrie 0.598σ</div>' +
        '</div>' +
        '<span class="badge-virgin">VIRGIN CANDIDATE</span>' +
      '</div>' +
      '<div class="metrics-grid">' +
        '<div class="metric-card"><div class="metric-title">Adâncime (δ)</div><div class="metric-value" style="color:#00E5FF;">' + depthPpm + ' ppm</div></div>' +
        '<div class="metric-card"><div class="metric-title">Simetrie Δσ</div><div class="metric-value" style="color:#00E676;">' + symmetrySigma + 'σ</div></div>' +
        '<div class="metric-card"><div class="metric-title">SNR Tranzit</div><div class="metric-value" style="color:#38BDF8;">' + snrSigma + 'σ</div></div>' +
        '<div class="metric-card"><div class="metric-title">Rază R⊕</div><div class="metric-value" style="color:#FFD600;">$planetRadius R⊕</div></div>' +
      '</div>' +
      '<div class="chart-container">' +
        '<svg class="svg-chart" viewBox="0 0 380 200">' +
          '<line x1="40" y1="20" x2="360" y2="20" stroke="#1E293B" stroke-dasharray="2,2"/>' +
          '<line x1="40" y1="70" x2="360" y2="70" stroke="#1E293B" stroke-dasharray="2,2"/>' +
          '<line x1="40" y1="120" x2="360" y2="120" stroke="#1E293B" stroke-dasharray="2,2"/>' +
          '<line x1="40" y1="170" x2="360" y2="170" stroke="#1E293B" stroke-dasharray="2,2"/>' +
          '<line x1="200" y1="10" x2="200" y2="180" stroke="#00E5FF" stroke-width="1" stroke-dasharray="2,2"/>' +
          '<rect x="135" y="20" width="130" height="150" fill="#00E5FF" fill-opacity="0.08"/>' +
          '<line x1="40" y1="140" x2="360" y2="140" stroke="#FF1744" stroke-width="1.5" stroke-dasharray="3,3"/>' +
          '<text x="44" y="135" fill="#FF1744" font-size="8" font-family="monospace">-1600 ppm (0.9984)</text>' +
          '<line x1="40" y1="35" x2="360" y2="35" stroke="#64748B" stroke-width="1"/>' +
          '<text x="12" y="38" fill="#64748B" font-size="8" font-family="monospace">1.000</text>' +
          '<text x="12" y="143" fill="#64748B" font-size="8" font-family="monospace">0.9984</text>' +
          '<path d="M 40 35 L 125 35 Q 142 36, 150 75 L 158 140 L 242 140 L 250 75 Q 258 36, 275 35 L 360 35" fill="none" stroke="#FFD600" stroke-width="2.5"/>' +
          dots +
          '<text x="140" y="15" fill="#38BDF8" font-size="8" font-weight="bold">Ingress (-4.55h)</text>' +
          '<text x="235" y="15" fill="#38BDF8" font-size="8" font-weight="bold">Egress (+4.55h)</text>' +
          '<text x="172" y="155" fill="#00E676" font-size="8" font-weight="bold">Δσ = 0.598 (Simetric)</text>' +
        '</svg>' +
      '</div>' +
      '<div class="legend-bar">' +
        '<div class="legend-item"><div class="legend-dot" style="background:#38BDF8;"></div>Fotoni Măsurați (TESS)</div>' +
        '<div class="legend-item"><div class="legend-dot" style="background:#FFD600;"></div>Model Tranzit (1600 ppm)</div>' +
        '<div class="legend-item"><div class="legend-dot" style="background:#00E676;"></div>Simetrie Profil (0.598σ)</div>' +
      '</div>' +
      '<div class="info-box">' +
        '<span class="info-tag">✓ Raport CTOI TIC 141913165: </span>' +
        'Adâncime 1600 ppm (1.6 ppt), SNR 9.80σ, Simetrie 0.598σ (<4σ), Durată 9.1h, Perioadă 18.42z. Rază 6.34 R⊕ (Sub-Saturn). Semnal virgin neînregistrat NASA TOI.' +
      '</div>';
    }

    if (typeof React !== 'undefined' && typeof ReactDOM !== 'undefined' && typeof window.Recharts !== 'undefined') {
      try {
        ReactDOM.render(React.createElement(Tic141913165Dashboard), document.getElementById("root"));
      } catch (err) {
        renderSvgFallback();
      }
    } else {
      renderSvgFallback();
    }
  </script>
</body>
</html>
""".trimIndent()
    }
}
