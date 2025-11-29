package kr.chatq.server.chatq_server.dto;

import java.util.List;

public class ChartResponse {
    private List<String> labels;
    private List<ChartDataset> datasets;

    public ChartResponse() {
    }

    public ChartResponse(List<String> labels, List<ChartDataset> datasets) {
        this.labels = labels;
        this.datasets = datasets;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<ChartDataset> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<ChartDataset> datasets) {
        this.datasets = datasets;
    }

    public static class ChartDataset {
        private String label;
        private List<Number> data;
        private String backgroundColor;
        private String borderColor;
        private Integer borderWidth;

        public ChartDataset() {
        }

        public ChartDataset(String label, List<Number> data, String backgroundColor, String borderColor, Integer borderWidth) {
            this.label = label;
            this.data = data;
            this.backgroundColor = backgroundColor;
            this.borderColor = borderColor;
            this.borderWidth = borderWidth;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public List<Number> getData() {
            return data;
        }

        public void setData(List<Number> data) {
            this.data = data;
        }

        public String getBackgroundColor() {
            return backgroundColor;
        }

        public void setBackgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

        public String getBorderColor() {
            return borderColor;
        }

        public void setBorderColor(String borderColor) {
            this.borderColor = borderColor;
        }

        public Integer getBorderWidth() {
            return borderWidth;
        }

        public void setBorderWidth(Integer borderWidth) {
            this.borderWidth = borderWidth;
        }
    }
}
