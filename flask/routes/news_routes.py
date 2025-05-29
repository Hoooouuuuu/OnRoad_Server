from flask import Blueprint, render_template, request
from crawler.news_crawler import get_all_news

bp = Blueprint("news", __name__)

@bp.route("/news")
def news_page():
    category_filter = request.args.get("category")
    articles = get_all_news()

    if category_filter and category_filter != "전체":
        # 카테고리 선택한 경우 → 해당 카테고리 전체 보여줌
        articles = [a for a in articles if category_filter in a["source"]]
    else:
        # 전체 선택한 경우 → 카테고리별로 하나씩만!
        seen = {}
        filtered = []
        for a in articles:
            # source = "메트로뉴스 - 지하철" → 카테고리만 추출
            parts = a["source"].split(" - ")
            if len(parts) == 2:
                cat = parts[1]
                if cat not in seen:
                    seen[cat] = True
                    filtered.append(a)
        articles = filtered

    return render_template("news.html", articles=articles, selected=category_filter or "전체")
