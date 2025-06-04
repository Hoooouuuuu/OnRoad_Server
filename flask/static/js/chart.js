document.addEventListener('DOMContentLoaded', () => {
  let globalSidoData = [];
  let globalTypeData = [];

  function getCheckedSidos() {
    return Array.from(document.querySelectorAll(".sido-checkbox:checked")).map(cb => cb.value);
  }

  function getSelectedChartTypes() {
    return Array.from(document.querySelectorAll("input[name='chartType']:checked")).map(el => el.value);
  }

  function getSelectedTypeCategories() {
    const el = document.getElementById("typeCategoryGroup");
    if (!el) return [];
    return Array.from(el.querySelectorAll("input[name='typeCategory']:checked")).map(el => el.value);
  }

  function updateSummaryCards() {
    const totals = {
      사고건수: 0,
      사망자수: 0,
      부상자수: 0
    };

    // 전체 데이터를 기준으로 합산
    globalSidoData.forEach(d => {
      const 항목 = d["항목"];
      const 값 = parseFloat(d["값"]) || 0;
      if (totals.hasOwnProperty(항목)) totals[항목] += 값;
    });

    document.getElementById("totalAccidents").textContent = totals["사고건수"].toLocaleString();
    document.getElementById("totalFatalities").textContent = totals["사망자수"].toLocaleString();
    document.getElementById("totalInjuries").textContent = totals["부상자수"].toLocaleString();
  }

  function drawChart() {
    const metricEl = document.getElementById("metricSelect");
    const yearEl = document.getElementById("yearSelect");
    if (!metricEl || !yearEl) return;

    const selectedMetric = metricEl.value;
    const selectedYear = yearEl.value;
    const selectedChartTypes = getSelectedChartTypes();
    const selectedSidos = getCheckedSidos();
    const selectedCategories = getSelectedTypeCategories();

    const filteredSido = globalSidoData.filter(d =>
      (selectedYear === "all" || d["연도"] === selectedYear) &&
      d["항목"] === selectedMetric &&
      selectedSidos.includes(d["시도"])
    );

    const filteredType = globalTypeData.filter(d =>
      (selectedYear === "all" || d["연도"] === selectedYear) &&
      selectedSidos.includes(d["시도"])
    );

    updateSummaryCards(); // 전체 기준 카드 업데이트

    let traces = [];
    const yearList = Array.from(new Set(filteredSido.map(d => d["연도"]))).sort();

    selectedChartTypes.forEach(chartType => {
      if (selectedYear === "all") {
        selectedSidos.forEach(sido => {
          const data = yearList.map(year => {
            return filteredSido
              .filter(d => d["연도"] === year && d["시도"] === sido)
              .reduce((sum, d) => sum + (parseFloat(d["값"]) || 0), 0);
          });

          traces.push({
            x: yearList,
            y: data,
            type: chartType,
            name: `${sido} (${chartType})`,
            marker: { line: { width: 1 } }
          });
        });
      } else {
        const x = selectedSidos;
        const y = selectedSidos.map(sido => {
          return filteredSido
            .filter(d => d["시도"] === sido)
            .reduce((sum, d) => sum + (parseFloat(d["값"]) || 0), 0);
        });

        traces.push({
          x,
          y,
          type: chartType,
          name: `${selectedMetric} (${chartType})`,
          marker: { line: { width: 1 } }
        });
      }
    });

    // 사고유형별 통계 차트 추가
    const categoryMap = new Map();
    filteredType.forEach(d => {
      const category = d["사고유형별"] || d["항목"] || "기타";
      if (selectedCategories.length === 0 || selectedCategories.includes(category)) {
        const value = parseFloat(d["값"] || 0);
        if (!categoryMap.has(category)) categoryMap.set(category, 0);
        categoryMap.set(category, categoryMap.get(category) + value);
      }
    });

    selectedChartTypes.forEach(type => {
      const x = selectedCategories.length ? selectedCategories : Array.from(categoryMap.keys());
      const y = x.map(cat => categoryMap.get(cat) || 0);
      traces.push({
        x,
        y,
        type,
        name: `사고유형 (${type})`,
        marker: { line: { width: 1 } }
      });
    });

    const chartEl = document.getElementById("chart");
    if (!traces.length) {
      chartEl.innerHTML = "📭 시각화할 데이터가 없습니다.";
      return;
    }

    // 동적 제목 구성
    const chartTitle = `${selectedYear === "all" ? "전체 연도" : selectedYear} ${selectedMetric} 추이`;

    Plotly.newPlot(chartEl, traces, {
      title: {
        text: chartTitle,
        font: { size: 22, color: "#333" }
      },
      margin: { t: 80, l: 60, r: 30, b: 80 },
      legend: {
        orientation: "h",
        yanchor: "bottom",
        y: -0.4,
        xanchor: "center",
        x: 0.5,
        font: { size: 11 }
      },
      xaxis: { title: "항목", tickangle: -30, tickfont: { size: 13 } },
      yaxis: { title: "건수", gridcolor: "#eaeaea", titlefont: { size: 15 } },
      plot_bgcolor: "#fff",
      paper_bgcolor: "#fff",
      height: 500
    }, { responsive: true });
  }

  function populateSelectors(sidoData, typeData) {
    const metricSet = new Set();
    const yearSet = new Set();
    const sidoSet = new Set();

    sidoData.forEach(d => {
      if (d["항목"]) metricSet.add(d["항목"]);
      if (d["연도"]) yearSet.add(d["연도"]);
      if (d["시도"] && d["시도"] !== "전국") sidoSet.add(d["시도"]);
    });

    const metricSelect = document.getElementById("metricSelect");
    const yearSelect = document.getElementById("yearSelect");
    const sidoGroup = document.getElementById("sidoGroup");
    const chartTypeGroup = document.getElementById("chartTypeGroup");

    if (!metricSelect || !yearSelect || !sidoGroup || !chartTypeGroup) {
      console.error("🚨 필수 DOM 요소가 없습니다.");
      return;
    }

    metricSelect.innerHTML = "";
    Array.from(metricSet).sort().forEach(metric => {
      const opt = document.createElement("option");
      opt.value = metric;
      opt.textContent = metric;
      metricSelect.appendChild(opt);
    });

    yearSelect.innerHTML = '<option value="all">전체</option>';
    Array.from(yearSet).sort().forEach(year => {
      const opt = document.createElement("option");
      opt.value = year;
      opt.textContent = year;
      yearSelect.appendChild(opt);
    });

    sidoGroup.innerHTML = "";
    Array.from(sidoSet).sort().forEach(sido => {
      const label = document.createElement("label");
      const checkbox = document.createElement("input");
      checkbox.type = "checkbox";
      checkbox.value = sido;
      checkbox.className = "sido-checkbox";
      checkbox.checked = true;
      label.appendChild(checkbox);
      label.append(` ${sido}`);
      sidoGroup.appendChild(label);
    });

    chartTypeGroup.innerHTML = "";
    ["bar", "line"].forEach(type => {
      const label = document.createElement("label");
      const checkbox = document.createElement("input");
      checkbox.type = "checkbox";
      checkbox.name = "chartType";
      checkbox.value = type;
      if (type === "bar") checkbox.checked = true;
      label.appendChild(checkbox);
      label.append(` ${type}`);
      chartTypeGroup.appendChild(label);
    });

    const typeCategoryGroup = document.createElement("div");
    typeCategoryGroup.id = "typeCategoryGroup";
    const typeCategories = Array.from(new Set(typeData.map(d => d["사고유형별"]).filter(Boolean)));
    typeCategories.forEach(cat => {
      const label = document.createElement("label");
      const checkbox = document.createElement("input");
      checkbox.type = "checkbox";
      checkbox.name = "typeCategory";
      checkbox.value = cat;
      checkbox.checked = true;
      label.appendChild(checkbox);
      label.append(` ${cat}`);
      typeCategoryGroup.appendChild(label);
    });
    chartTypeGroup.appendChild(typeCategoryGroup);
  }

  // ✅ 데이터 로딩 및 초기화
  fetch("/chart/data/combined")
    .then(res => res.json())
    .then(data => {
      globalSidoData = data["시도_통계"] || [];
      globalTypeData = data["사고유형_통계"] || [];

      window.globalSidoData = globalSidoData;
      window.globalTypeData = globalTypeData;

      populateSelectors(globalSidoData, globalTypeData);
      drawChart();
    })
    .catch(err => {
      const chartEl = document.getElementById("chart");
      if (chartEl) {
        chartEl.innerText = "🚨 차트 로딩 실패: " + err.message;
      }
      console.error("🚨 차트 로딩 중 에러 발생:", err);
    });

  // ✅ 필터 이벤트 연결
  document.getElementById("metricSelect")?.addEventListener("change", drawChart);
  document.getElementById("yearSelect")?.addEventListener("change", drawChart);
  document.getElementById("sidoGroup")?.addEventListener("change", drawChart);
  document.getElementById("chartTypeGroup")?.addEventListener("change", drawChart);
  document.body.addEventListener("change", e => {
    if (e.target.name === "typeCategory") drawChart();
  });
});
