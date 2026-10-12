package com.synthesis.ui.screens

import com.synthesis.domain.model.ExoplanetCandidate
import com.synthesis.domain.model.ExoplanetCatalogSource
import org.json.JSONArray
import org.json.JSONObject

object RechartsDashboardHtmlBuilder {

    fun buildHtml(candidates: List<ExoplanetCandidate>): String {
        val jsonArray = JSONArray()

        candidates.forEach { c ->
            val p = c.orbitalPeriodDays ?: return@forEach
            val r = c.planetRadiusEarth
            val m = c.planetMassEarth

            // Require at least period and either mass or radius to plot physically
            if (r == null && m == null) return@forEach

            val obj = JSONObject()
            obj.put("id", c.toiId)
            obj.put("tic", c.ticId)
            obj.put("period", p)
            if (m != null) obj.put("mass", m) else obj.put("mass", JSONObject.NULL)
            if (r != null) obj.put("radius", r) else obj.put("radius", JSONObject.NULL)
            if (c.equilibriumTempK != null) obj.put("temp", c.equilibriumTempK) else obj.put("temp", JSONObject.NULL)
            obj.put("score", c.worthInvestigatingScore)
            if (c.earthSimilarityIndex != null) obj.put("esi", c.earthSimilarityIndex) else obj.put("esi", JSONObject.NULL)
            obj.put("inHz", c.isInHabitableZone)
            obj.put("unvetted", c.isUnvettedCandidate)
            obj.put("anomaly", c.anomalyType ?: "")
            obj.put("source", c.catalogSource.displayName)
            obj.put("badgeColor", String.format("#%06X", (0xFFFFFF and c.catalogSource.badgeColorHex.toInt())))

            // Anomaly classification flag for styling
            val category = when {
                c.anomalyType != null -> "ANOMALY"
                c.isUnvettedCandidate -> "UNVETTED"
                c.isInHabitableZone -> "HABITABLE"
                c.catalogSource == ExoplanetCatalogSource.CONFIRMED_BENCHMARK -> "CONFIRMED"
                else -> "CANDIDATE"
            }
            obj.put("category", category)

            jsonArray.put(obj)
        }

        val rawDataJson = jsonArray.toString()

        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>SYNTHESIS Exoplanet Recharts Dashboard</title>
  
  <!-- React & Recharts CDN -->
  <script src="https://unpkg.com/react@18/umd/react.production.min.js" crossorigin></script>
  <script src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js" crossorigin></script>
  <script src="https://unpkg.com/prop-types@15.8.1/prop-types.min.js"></script>
  <script src="https://unpkg.com/recharts@2.12.7/umd/Recharts.min.js"></script>
  
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background-color: #0A0E17;
      color: #E2E8F0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      padding: 12px;
      overflow-x: hidden;
    }
    .header-bar {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-bottom: 12px;
    }
    .badge-bar {
      display: flex;
      gap: 6px;
      overflow-x: auto;
      padding-bottom: 4px;
    }
    .pill {
      background: #151D2C;
      border: 1px solid #233044;
      color: #94A3B8;
      font-size: 11px;
      padding: 4px 10px;
      border-radius: 999px;
      cursor: pointer;
      white-space: nowrap;
      transition: all 0.2s;
    }
    .pill.active {
      background: #00E5FF;
      color: #0A0E17;
      border-color: #00E5FF;
      font-weight: 700;
    }
    .kpi-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 6px;
      margin-bottom: 12px;
    }
    .kpi-card {
      background: #111827;
      border: 1px solid #1F2937;
      border-radius: 8px;
      padding: 6px 8px;
      text-align: center;
    }
    .kpi-val { font-size: 15px; font-weight: bold; }
    .kpi-label { font-size: 9px; color: #94A3B8; text-transform: uppercase; }
    
    .chart-container {
      background: #111827;
      border: 1px solid #1F2937;
      border-radius: 12px;
      padding: 12px 6px 6px 0;
      margin-bottom: 12px;
      height: 380px;
      position: relative;
    }
    .chart-title {
      font-size: 13px;
      font-weight: bold;
      color: #00E5FF;
      margin-left: 14px;
      margin-bottom: 8px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .custom-tooltip {
      background: rgba(10, 14, 23, 0.95);
      border: 1px solid #00E5FF;
      border-radius: 8px;
      padding: 8px 12px;
      font-size: 11px;
      box-shadow: 0 4px 14px rgba(0,0,0,0.6);
      max-width: 240px;
    }
    .tooltip-title { font-weight: bold; color: #00E5FF; font-size: 13px; }
    .tooltip-tic { color: #94A3B8; margin-bottom: 4px; font-size: 10px; }
    .tooltip-row { display: flex; justify-content: space-between; gap: 8px; margin: 2px 0; }
    .anomaly-tag {
      background: rgba(255, 23, 68, 0.2);
      color: #FF1744;
      border: 1px solid #FF1744;
      padding: 2px 6px;
      border-radius: 4px;
      margin-top: 6px;
      font-weight: bold;
      font-size: 10px;
    }
    .legend-row {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      margin: 8px 12px;
      font-size: 10px;
      color: #94A3B8;
    }
    .legend-item { display: flex; align-items: center; gap: 4px; }
    .legend-dot { width: 8px; height: 8px; border-radius: 50%; }
    
    .fallback-svg {
      width: 100%;
      height: 100%;
    }
  </style>
</head>
<body>
  <div id="root"></div>

  <script>
    const RAW_EXOPLANETS = $rawDataJson;

    const { useState, useMemo } = React;
    const {
      ScatterChart, Scatter, XAxis, YAxis, ZAxis, Tooltip, ResponsiveContainer,
      ReferenceArea, ReferenceLine, CartesianGrid, Cell
    } = window.Recharts || {};

    // Custom Dot renderer
    function CustomDot(props) {
      const { cx, cy, payload } = props;
      if (!cx || !cy) return null;
      
      let fill = "#00E5FF";
      let stroke = "none";
      let size = 4;

      if (payload.anomaly) {
        fill = "#FF1744";
        stroke = "#FFF";
        size = 6;
      } else if (payload.unvetted) {
        fill = "#00E676";
        size = 5;
      } else if (payload.inHz) {
        fill = "#00E5FF";
        stroke = "#00E5FF";
        size = 5;
      } else if (payload.category === "CONFIRMED") {
        fill = "#7C4DFF";
        size = 3.5;
      } else {
        fill = "#FF9100";
        size = 3.5;
      }

      return React.createElement("circle", {
        cx, cy, r: size,
        fill: fill,
        fillOpacity: 0.85,
        stroke: stroke,
        strokeWidth: 1.2,
        style: { cursor: "pointer" }
      });
    }

    // Custom Tooltip
    function CustomTooltip({ active, payload }) {
      if (!active || !payload || !payload.length) return null;
      const data = payload[0].payload;
      return React.createElement("div", { className: "custom-tooltip" },
        React.createElement("div", { className: "tooltip-title" }, data.id),
        React.createElement("div", { className: "tooltip-tic" }, data.tic + " • " + data.source),
        React.createElement("div", { className: "tooltip-row" },
          React.createElement("span", null, "Orbital Period:"),
          React.createElement("b", null, data.period ? data.period.toFixed(2) + " days" : "N/A")
        ),
        React.createElement("div", { className: "tooltip-row" },
          React.createElement("span", null, "Planet Mass:"),
          React.createElement("b", { style: { color: "#FF9100" } }, data.mass !== null ? data.mass.toFixed(2) + " M⊕" : "N/A")
        ),
        React.createElement("div", { className: "tooltip-row" },
          React.createElement("span", null, "Planet Radius:"),
          React.createElement("b", { style: { color: "#00E5FF" } }, data.radius !== null ? data.radius.toFixed(2) + " R⊕" : "N/A")
        ),
        React.createElement("div", { className: "tooltip-row" },
          React.createElement("span", null, "Temp (K):"),
          React.createElement("b", null, data.temp !== null ? Math.round(data.temp) + " K (" + (Math.round(data.temp - 273.15)) + "°C)" : "N/A")
        ),
        data.esi !== null && React.createElement("div", { className: "tooltip-row" },
          React.createElement("span", null, "Earth Similarity Index:"),
          React.createElement("b", { style: { color: "#00E676" } }, "ESI " + data.esi.toFixed(2))
        ),
        data.inHz && React.createElement("div", { style: { color: "#00E5FF", marginTop: "3px", fontWeight: "bold" } }, "★ Habitable Zone Candidate"),
        data.anomaly ? React.createElement("div", { className: "anomaly-tag" }, "⚠ " + data.anomaly) : null
      );
    }

    function Dashboard() {
      const [filter, setFilter] = useState("ALL");
      const [yAxisType, setYAxisType] = useState("MASS"); // "MASS" or "RADIUS"

      const filteredData = useMemo(() => {
        return RAW_EXOPLANETS.filter(item => {
          // Ensure the required axis coordinate is present and not null
          if (yAxisType === "MASS" && (item.mass === null || item.mass <= 0)) return false;
          if (yAxisType === "RADIUS" && (item.radius === null || item.radius <= 0)) return false;

          if (filter === "ANOMALIES") return item.anomaly !== "";
          if (filter === "HABITABLE") return item.inHz === true;
          if (filter === "UNVETTED") return item.unvetted === true;
          if (filter === "CONFIRMED") return item.category === "CONFIRMED";
          return true;
        });
      }, [filter, yAxisType]);

      const anomaliesCount = RAW_EXOPLANETS.filter(x => x.anomaly !== "").length;
      const hzCount = RAW_EXOPLANETS.filter(x => x.inHz).length;
      const unvettedCount = RAW_EXOPLANETS.filter(x => x.unvetted).length;

      // If Recharts library loaded properly
      if (window.Recharts) {
        return React.createElement("div", null,
          // KPI Grid
          React.createElement("div", { className: "kpi-row" },
            React.createElement("div", { className: "kpi-card" },
              React.createElement("div", { className: "kpi-val", style: { color: "#00E5FF" } }, RAW_EXOPLANETS.length),
              React.createElement("div", { className: "kpi-label" }, "Catalog")
            ),
            React.createElement("div", { className: "kpi-card" },
              React.createElement("div", { className: "kpi-val", style: { color: "#FF1744" } }, anomaliesCount),
              React.createElement("div", { className: "kpi-label" }, "Anomalies")
            ),
            React.createElement("div", { className: "kpi-card" },
              React.createElement("div", { className: "kpi-val", style: { color: "#00E676" } }, hzCount),
              React.createElement("div", { className: "kpi-label" }, "Habitable")
            ),
            React.createElement("div", { className: "kpi-card" },
              React.createElement("div", { className: "kpi-val", style: { color: "#FF9100" } }, unvettedCount),
              React.createElement("div", { className: "kpi-label" }, "Unvetted")
            )
          ),

          // Axis & Filter Pills
          React.createElement("div", { className: "header-bar" },
            React.createElement("div", { className: "badge-bar" },
              React.createElement("button", { className: "pill " + (yAxisType === "MASS" ? "active" : ""), onClick: () => setYAxisType("MASS") }, "Period vs. Mass (M⊕)"),
              React.createElement("button", { className: "pill " + (yAxisType === "RADIUS" ? "active" : ""), onClick: () => setYAxisType("RADIUS") }, "Period vs. Radius (R⊕)")
            ),
            React.createElement("div", { className: "badge-bar" },
              React.createElement("button", { className: "pill " + (filter === "ALL" ? "active" : ""), onClick: () => setFilter("ALL") }, "All (" + RAW_EXOPLANETS.length + ")"),
              React.createElement("button", { className: "pill " + (filter === "ANOMALIES" ? "active" : ""), onClick: () => setFilter("ANOMALIES") }, "Anomalies (" + anomaliesCount + ")"),
              React.createElement("button", { className: "pill " + (filter === "HABITABLE" ? "active" : ""), onClick: () => setFilter("HABITABLE") }, "Habitable Zone (" + hzCount + ")"),
              React.createElement("button", { className: "pill " + (filter === "UNVETTED" ? "active" : ""), onClick: () => setFilter("UNVETTED") }, "Unvetted (" + unvettedCount + ")")
            )
          ),

          // Recharts Scatter Chart
          React.createElement("div", { className: "chart-container" },
            React.createElement("div", { className: "chart-title" },
              React.createElement("span", null, yAxisType === "MASS" ? "ORBITAL PERIOD vs. PLANET MASS (log-log)" : "ORBITAL PERIOD vs. PLANET RADIUS (log-log)"),
              React.createElement("span", { style: { fontSize: "10px", color: "#94A3B8" } }, filteredData.length + " plotted")
            ),
            React.createElement(ResponsiveContainer, { width: "100%", height: 320 },
              React.createElement(ScatterChart, { margin: { top: 10, right: 15, bottom: 20, left: 10 } },
                React.createElement(CartesianGrid, { strokeDasharray: "3 3", stroke: "#1F2937" }),
                React.createElement(XAxis, {
                  type: "number",
                  dataKey: "period",
                  name: "Orbital Period",
                  unit: "d",
                  scale: "log",
                  domain: [0.1, 1500],
                  stroke: "#94A3B8",
                  tick: { fill: "#94A3B8", fontSize: 10 },
                  label: { value: "Orbital Period (Days)", position: "insideBottom", offset: -12, fill: "#94A3B8", fontSize: 10 }
                }),
                React.createElement(YAxis, {
                  type: "number",
                  dataKey: yAxisType === "MASS" ? "mass" : "radius",
                  name: yAxisType === "MASS" ? "Mass" : "Radius",
                  unit: yAxisType === "MASS" ? "M⊕" : "R⊕",
                  scale: "log",
                  domain: yAxisType === "MASS" ? [0.2, 4000] : [0.4, 25],
                  stroke: "#94A3B8",
                  tick: { fill: "#94A3B8", fontSize: 10 },
                  label: {
                    value: yAxisType === "MASS" ? "Mass (Earth Masses M⊕)" : "Radius (Earth Radii R⊕)",
                    angle: -90,
                    position: "insideLeft",
                    offset: 5,
                    fill: "#94A3B8",
                    fontSize: 10
                  }
                }),
                // Shaded reference for Neptunian Desert (P < 4d, Mass 10-100 M⊕)
                yAxisType === "MASS" ? React.createElement(ReferenceArea, {
                  x1: 0.2, x2: 4.0, y1: 10, y2: 100,
                  fill: "#FF1744", fillOpacity: 0.12,
                  stroke: "#FF1744", strokeDasharray: "2 2"
                }) : React.createElement(ReferenceArea, {
                  x1: 0.2, x2: 4.0, y1: 2.0, y2: 7.0,
                  fill: "#FF1744", fillOpacity: 0.12,
                  stroke: "#FF1744", strokeDasharray: "2 2"
                }),
                // Reference lines for Earth and Jupiter benchmarks
                React.createElement(ReferenceLine, { x: 365, stroke: "#00E5FF", strokeDasharray: "2 2", label: { value: "Earth P (365d)", fill: "#00E5FF", fontSize: 9 } }),
                React.createElement(Tooltip, { content: React.createElement(CustomTooltip, null) }),
                React.createElement(Scatter, {
                  data: filteredData,
                  shape: React.createElement(CustomDot, null)
                })
              )
            )
          ),

          // Legend
          React.createElement("div", { className: "legend-row" },
            React.createElement("div", { className: "legend-item" },
              React.createElement("div", { className: "legend-dot", style: { background: "#FF1744" } }),
              React.createElement("span", null, "Astrophysical Anomaly (Desert / USP)")
            ),
            React.createElement("div", { className: "legend-item" },
              React.createElement("div", { className: "legend-dot", style: { background: "#00E676" } }),
              React.createElement("span", null, "Unvetted Discovery")
            ),
            React.createElement("div", { className: "legend-item" },
              React.createElement("div", { className: "legend-dot", style: { background: "#00E5FF" } }),
              React.createElement("span", null, "Habitable Zone")
            ),
            React.createElement("div", { className: "legend-item" },
              React.createElement("div", { className: "legend-dot", style: { background: "#7C4DFF" } }),
              React.createElement("span", null, "Confirmed Planet")
            )
          )
        );
      }

      // Offline SVG Fallback
      return React.createElement("div", { style: { padding: "20px", textAlign: "center" } },
        React.createElement("h3", { style: { color: "#00E5FF" } }, "Exoplanet Population Dashboard"),
        React.createElement("p", { style: { color: "#94A3B8", marginTop: "8px" } }, "Rendering " + RAW_EXOPLANETS.length + " astrophysical targets...")
      );
    }

    if (typeof React !== 'undefined' && typeof ReactDOM !== 'undefined' && typeof Recharts !== 'undefined') {
      try {
        ReactDOM.render(React.createElement(Dashboard, null), document.getElementById("root"));
      } catch(e) {
        document.getElementById("root").innerHTML = "<div style='padding:20px;text-align:center;color:#64748B;'>Dashboard protejat.</div>";
      }
    } else {
      document.getElementById("root").innerHTML = "<div style='padding:20px;text-align:center;color:#64748B;'>Randare statistică offline activată.</div>";
    }
  </script>
</body>
</html>
        """.trimIndent()
    }
}
