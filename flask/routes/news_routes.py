from flask import Blueprint, render_template, request
from crawler.news_crawler import get_all_news, get_popular_news

bp = Blueprint("news", __name__)

@bp.route("/news")
def news_page():
    category_filter = request.args.get("category")
    all_articles = get_all_news()
    articles = all_articles

    if category_filter and category_filter != "전체":
        articles = [a for a in all_articles if category_filter in a["source"]]
    else:
        seen = {}
        filtered = []
        for a in all_articles:
            parts = a["source"].split(" - ")
            if len(parts) == 2:
                cat = parts[1]
                if cat not in seen:
                    seen[cat] = True
                    filtered.append(a)
        articles = filtered

    keywords = ["속보", "긴급", "파업", "지연", "지하철", "사고", "정전", "무정차"]
    ticker = []
    for a in all_articles[:10]:
        if any(k in a["title"] for k in keywords):
            ticker.append(a["title"])
        if len(ticker) >= 5:
            break

    try:
        popular_articles = get_popular_news()
    except Exception as e:
        print("🔥 인기기사 오류:", e)
        popular_articles = []

    return render_template(
        "news.html",
        articles=articles,
        selected=category_filter or "전체",
        ticker=ticker,
        popular_articles=popular_articles
    )
