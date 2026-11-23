# Metric view component boundaries

[Handbook home](README.md) · [Workbench UI](workbench-ui.md)

`MetricViewController` is the coordinator. It may talk to the filter/header only through
`QueryEditorPort` and to the legend through `ChartLegendPort`. The chart owns rendering, axes,
zoom and series state; the legend owns legend presentation and interaction; the query editor owns
filter input and its actions. FXML connects concrete controllers, but Java dependencies point at
these ports.

Put new query controls behind `QueryEditorPort`, chart mechanics in `uitoolkit.chart`, and legend
behaviour behind `ChartLegendPort`. Do not let header or legend call each other. Cross-component
behaviour is an action/event coordinated by `MetricViewController`. `LayerArchitectureTest`
protects these two concrete-dependency boundaries.
