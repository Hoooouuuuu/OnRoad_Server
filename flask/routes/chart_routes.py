import os
import pandas as pd
import numpy as np
from flask import Blueprint, jsonify, render_template
import traceback

bp = Blueprint('chart', __name__, url_prefix='/chart')

@bp.route("/")
def chart_page():
  return render_template("pages/statistics/chart.html")

@bp.route("/data/combined")
def chart_data_combined():
  try:
    data_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'data'))

    # 📄 파일1: 사고유형 + 도로종류 + 항목 통계
    file1_path = os.path.join(data_dir, "132_DT_V_MOTA_014_20250527122437.csv")
    df_type = pd.read_csv(file1_path, encoding="utf-8")
    df_type = df_type.drop(columns=["Unnamed: 10"], errors="ignore")
    df_type_melted = df_type.melt(
      id_vars=["사고유형별", "도로종류별", "월별", "항목", "단위"],
      var_name="연도",
      value_name="값"
    )
    df_type_melted["시도"] = "전국"
    df_type_ready = df_type_melted[["연도", "항목", "시도", "값"]].dropna()

    # 📄 파일2: 시도별 월별 교통사고 통계
    file2_path = os.path.join(data_dir, "시도별_교통사고_20250527122615.csv")
    df_region = pd.read_csv(file2_path, encoding="utf-8", header=[0, 1])
    df_region.columns = ['시도', '월'] + [
      f"{col[0]}_{col[1].split()[0]}" for col in df_region.columns[2:]
    ]
    df_region_melted = df_region.melt(
      id_vars=["시도", "월"],
      var_name="연도_항목",
      value_name="값"
    )
    df_region_melted[["연도", "항목"]] = df_region_melted["연도_항목"].str.extract(r"(\d{4})_(.*)")
    df_region_melted["값"] = pd.to_numeric(df_region_melted["값"], errors="coerce")
    df_region_ready = df_region_melted[["연도", "항목", "시도", "값"]].dropna()

    return jsonify({
      "시도_통계": df_region_ready.to_dict(orient="records"),
      "사고유형_통계": df_type_ready.to_dict(orient="records")
    })

  except Exception as e:
    print("\U0001F525 /chart/data/combined 에러 발생:")
    traceback.print_exc()
    return jsonify({"error": str(e)}), 500
