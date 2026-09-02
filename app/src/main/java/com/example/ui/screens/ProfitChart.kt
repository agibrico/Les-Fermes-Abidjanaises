package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.entity.FarmTransaction
import java.util.Calendar
import java.util.Date

@Composable
fun MonthlyProfitChartWebView(transactions: List<FarmTransaction>) {
    val calendar = Calendar.getInstance()
    val monthNames = listOf("Jan", "Fév", "Mar", "Avr", "Mai", "Jun", "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc")

    val grouped = transactions.groupBy {
        calendar.timeInMillis = it.date
        val m = calendar.get(Calendar.MONTH)
        val y = calendar.get(Calendar.YEAR)
        Pair(y * 12 + m, "${monthNames[m]} ${y % 100}")
    }.mapValues { entry ->
        val rev = entry.value.filter { it.type == "IN" }.sumOf { it.amount }
        val exp = entry.value.filter { it.type == "OUT" }.sumOf { it.amount }
        Pair(rev, exp)
    }.toList().sortedBy { it.first.first }

    // Fallback preview data if no transactions
    val chartData = if (grouped.isNotEmpty()) {
        grouped.map { (key, value) ->
            "{\"month\": \"${key.second}\", \"revenues\": ${value.first}, \"expenses\": ${value.second}}"
        }
    } else {
        listOf(
            "{\"month\": \"Avr 26\", \"revenues\": 1200000, \"expenses\": 850000}",
            "{\"month\": \"Mai 26\", \"revenues\": 1800000, \"expenses\": 1100000}",
            "{\"month\": \"Jun 26\", \"revenues\": 2100000, \"expenses\": 1300000}",
            "{\"month\": \"Jul 26\", \"revenues\": 2500000, \"expenses\": 1450000}"
        )
    }

    val dataJson = chartData.joinToString(prefix = "[", postfix = "]") { it }

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    margin: 0;
                    padding: 8px 12px;
                    background-color: #fcfdf6;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    color: #1a1c18;
                }
                .chart-container {
                    width: 100%;
                    height: 200px;
                }
                .bar-rev {
                    fill: #2E7D32;
                }
                .bar-exp {
                    fill: #C62828;
                }
                .axis text {
                    font-size: 11px;
                    fill: #43493e;
                }
                .axis path, .axis line {
                    stroke: #c2c9bd;
                }
                .legend {
                    display: flex;
                    justify-content: center;
                    margin-bottom: 12px;
                    font-size: 11px;
                    color: #43493e;
                }
                .legend-item {
                    display: flex;
                    align-items: center;
                    margin: 0 10px;
                }
                .legend-color {
                    width: 12px;
                    height: 12px;
                    margin-right: 6px;
                    border-radius: 3px;
                }
                .grid line {
                    stroke: #e1e4da;
                    stroke-opacity: 0.7;
                    shape-rendering: crispEdges;
                }
            </style>
            <script src="https://d3js.org/d3.v7.min.js"></script>
        </head>
        <body>
            <div class="legend">
                <div class="legend-item">
                    <div class="legend-color" style="background-color: #2E7D32;"></div>
                    <span>Revenus (Ventes)</span>
                </div>
                <div class="legend-item">
                    <div class="legend-color" style="background-color: #C62828;"></div>
                    <span>Dépenses (Intrants)</span>
                </div>
            </div>
            <div id="chart" class="chart-container"></div>

            <script>
                const data = $dataJson;

                const drawChart = () => {
                    const container = document.getElementById('chart');
                    const width = container.clientWidth;
                    const height = container.clientHeight;

                    container.innerHTML = '';

                    const margin = {top: 10, right: 10, bottom: 30, left: 55};
                    const chartWidth = width - margin.left - margin.right;
                    const chartHeight = height - margin.top - margin.bottom;

                    const svg = d3.select("#chart")
                        .append("svg")
                        .attr("width", "100%")
                        .attr("height", "100%")
                        .attr("viewBox", "0 0 " + width + " " + height)
                        .append("g")
                        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

                    const x0 = d3.scaleBand()
                        .domain(data.map(d => d.month))
                        .rangeRound([0, chartWidth])
                        .paddingInner(0.25);

                    const x1 = d3.scaleBand()
                        .domain(['revenues', 'expenses'])
                        .rangeRound([0, x0.bandwidth()])
                        .padding(0.05);

                    const maxY = d3.max(data, d => Math.max(d.revenues, d.expenses)) || 100000;

                    const y = d3.scaleLinear()
                        .domain([0, maxY * 1.1])
                        .rangeRound([chartHeight, 0]);

                    // Grid lines
                    svg.append("g")
                        .attr("class", "grid")
                        .call(d3.axisLeft(y)
                            .ticks(5)
                            .tickSize(-chartWidth)
                            .tickFormat("")
                        );

                    // X Axis
                    svg.append("g")
                        .attr("class", "axis")
                        .attr("transform", "translate(0," + chartHeight + ")")
                        .call(d3.axisBottom(x0));

                    // Y Axis
                    svg.append("g")
                        .attr("class", "axis")
                        .call(d3.axisLeft(y)
                            .ticks(5)
                            .tickFormat(d => {
                                if (d >= 1000000) return (d / 1000000).toFixed(1) + 'M';
                                if (d >= 1000) return (d / 1000).toFixed(0) + 'k';
                                return d;
                            })
                        );

                    // Draw bars
                    svg.append("g")
                        .selectAll("g")
                        .data(data)
                        .join("g")
                        .attr("transform", d => "translate(" + x0(d.month) + ", 0)")
                        .selectAll("rect")
                        .data(d => [
                            {key: 'revenues', value: d.revenues, class: 'bar-rev'},
                            {key: 'expenses', value: d.expenses, class: 'bar-exp'}
                        ])
                        .join("rect")
                        .attr("x", d => x1(d.key))
                        .attr("y", d => y(d.value))
                        .attr("width", x1.bandwidth())
                        .attr("height", d => chartHeight - y(d.value))
                        .attr("class", d => d.class)
                        .attr("rx", 3)
                        .attr("ry", 3);
                };

                drawChart();
                window.addEventListener('resize', drawChart);
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    )
}
