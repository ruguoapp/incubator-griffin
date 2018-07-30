/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
import { Injectable } from "@angular/core";

@Injectable()
export class ChartService {
  constructor() {}

  formatter_value(value, index) {
    if (value < 1000) {
      return value;
    } else {
      return value / 1000;
    }
  }

  formatter_yaxis_name(metric) {
    return "accuracy (%)";
  }

  getUTCTimeStamp(timestamp) {
    var TzOffset = new Date(timestamp).getTimezoneOffset() / 60;
    return timestamp - TzOffset * 60 * 60 * 1000;
  }

  getTooltip(params) {
    var result = "";
    if (params.length > 0) {
      result =
        new Date(this.getUTCTimeStamp(params[0].data[0]))
          .toUTCString()
          .replace("GMT", "") +
        "<br /> Value : " +
        params[0].data[1];
    }
    return result;
  }

  getTooltipPosition(point, params, dom) {
    return [point[0] / 2, point[1] / 2];
  }

  formatTimeStamp(timestamp) {
    return timestamp;
  }

  getMetricData(metric) {
    var data = {"name": "unknown", "data": [], "min": +Infinity, "max": -Infinity};
    var valueGetter : (any) => number = valueGetter = (v) => 0;
    if (metric.details) {
      var chartData = metric.details;
      if (chartData.length >= 1) {
        console.log(chartData[0].value);
        if (chartData[0].value.hasOwnProperty('matched') && chartData[0].value.hasOwnProperty('total')) {
          data.name = "accuracy%";
          valueGetter = (v) => parseFloat((v.matched / v.total * 100).toFixed(2));
        } else if (Object.getOwnPropertyNames(chartData[0].value).length == 1) {
          data.name = Object.getOwnPropertyNames(chartData[0].value)[0];
          valueGetter = (v) => parseFloat(v[data.name]);
        } else {
          data.name = "unknown";
          valueGetter = (v) => 0;
          console.log("Too many properties");
          console.log(chartData[0].value);
        }
      }
      for (var i = 0; i < chartData.length; i++) {
        let newValue = valueGetter(chartData[i].value);
        data.min = Math.min(data.min, newValue);
        data.max = Math.max(data.max, newValue);
        data.data.push([
          this.formatTimeStamp(chartData[i].tmst),
          newValue
        ]);
      }
    }
    data.data.sort(function(a, b) {
      return a[0] - b[0];
    });
    return data;
  }

  getOptionSide(metric) {
    var data = this.getMetricData(metric);
    var self = this;
    var option = {
      title: {
        show: false
      },
      backgroundColor: "transparent",
      grid: {
        right: "5%",
        left: "5%",
        bottom: "5%",
        top: 30,
        containLabel: true
      },
      tooltip: {
        trigger: "axis",
        formatter: function(params) {
          return self.getTooltip(params);
        }
      },
      xAxis: {
        type: "time",
        splitLine: {
          show: false
        },
        splitNumber: 2,
        axisLine: {
          lineStyle: {
            color: "white"
          }
        },
        axisLabel: {
          color: "white"
        },
        nameTextStyle: {
          color: "white"
        }
      },
      yAxis: {
        type: "value",
        scale: true,
        splitNumber: 2,
        name: data.name,
        axisLabel: {
          formatter: this.formatter_value,
          color: "white"
        },
        splitLine: {
          lineStyle: {
            type: "dashed"
          }
        },
        axisLine: {
          lineStyle: {
            color: "white"
          }
        },
        nameTextStyle: {
          color: "white"
        }
      },
      series: {}
    };
    option.series = this.getSeries(metric);
    return option;
  }

  getSeriesCount(metric) {
    var series = [];
    var data = this.getMetricData(metric);
    series.push({
      type: "line",
      data: data.data,
      smooth: true,
      lineStyle: {
        normal: {
          color: "#d48265"
        }
      },
      itemStyle: {
        normal: {
          color: "#d48265"
        }
      }
    });
    return series;
  }

  getSeries(metric) {
    var series = {};
    series = this.getSeriesCount(metric);
    return series;
  }

  getOptionThum(metric) {
    var data = this.getMetricData(metric);
    var trail = '...';
    var self = this;
    var option = {
      title: {
        text: metric.name.length > 10 ? metric.name.substring(0, 10) + trail : metric.name,
        left: "center",
        textStyle: {
          fontWeight: "normal",
          fontSize: 15,
          color: "white"
        }
      },
      backgroundColor: "transparent",
      grid: {
        right: "7%",
        left: "5%",
        bottom: "5%",
        containLabel: true
      },
      tooltip: {
        trigger: "axis",
        formatter: function(params) {
          return self.getTooltip(params);
        },
        position: function(point, params, dom) {
          return self.getTooltipPosition(point, params, dom);
        }
      },
      xAxis: {
        axisLine: {
          lineStyle: {
            color: "white"
          }
        },
        type: "time",
        splitLine: {
          show: false
        },
        axisLabel: {
          color: "white"
        },
        nameTextStyle: {
          color: "white"
        },
        splitNumber: 2
      },

      yAxis: {
        type: "value",
        scale: true,
        name: data.name,
        axisLabel: {
          formatter: this.formatter_value,
          color: "white"
        },
        splitLine: {
          lineStyle: {
            type: "dashed"
          }
        },
        axisLine: {
          lineStyle: {
            color: "white"
          }
        },
        nameTextStyle: {
          color: "white"
        },
        splitNumber: 2
      },
      series: {}
    };
    option.series = this.getSeries(metric);
    return option;
    // }
  }

  getOptionBig(metric) {
    var data = this.getMetricData(metric);
    var self = this;
    var option = {
      title: {
        text: metric.name,
        link: "/measure/" + metric.name,
        target: "self",
        left: "center",
        textStyle: {
          fontSize: 25,
          color: "white"
        }
      },
      grid: {
        right: "2%",
        left: "2%",
        containLabel: true
      },
      dataZoom: [
        {
          type: "inside",
          start: 0,
          throttle: 50
        },
        {
          show: true,
          start: 0
        }
      ],
      tooltip: {
        trigger: "axis",
        formatter: function(params) {
          return self.getTooltip(params);
        }
      },
      xAxis: {
        axisLine: {
          lineStyle: {
            color: "white"
          }
        },
        type: "time",
        splitLine: {
          show: false
        },
        axisLabel: {
          color: "white"
        },
        nameTextStyle: {
          color: "white"
        }
      },
      yAxis: {
        type: "value",
        scale: true,
        splitLine: {
          lineStyle: {
            type: "dashed"
          }
        },
        name: data.name,
        axisLabel: {
          formatter: null,
          color: "white"
        },
        axisLine: {
          lineStyle: {
            color: "white"
          }
        },
        nameTextStyle: {
          color: "white"
        }
      },
      animation: true,
      series: {}
    };
    option.series = this.getSeries(metric);
    return option;
  }
}

