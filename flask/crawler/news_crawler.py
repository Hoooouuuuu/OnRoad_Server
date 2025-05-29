import requests
from bs4 import BeautifulSoup
from datetime import datetime

def get_article_date(detail_url):
    """
    상세 페이지에서 <meta property="article:published_time"> 태그에서 날짜 추출
    """
    headers = {"User-Agent": "Mozilla/5.0"}
    res = requests.get(detail_url, headers=headers)
    soup = BeautifulSoup(res.text, "html.parser")

    meta_tag = soup.find("meta", attrs={"property": "article:published_time"})
    if meta_tag:
        iso_date = meta_tag.get("content")  # 예: '2025-05-23T11:43:36+09:00'
        return iso_date[:10].replace("-", ".")  # → '2025.05.23'
    return ""

def get_metro_news(url, category):
    headers = {"User-Agent": "Mozilla/5.0"}
    response = requests.get(url, headers=headers)
    soup = BeautifulSoup(response.text, "html.parser")
    articles = []

    rows = soup.select("div.boardList ul li")
    for row in rows:
        link_tag = row.select_one("a")
        title_tag = row.select_one("div.boardListSubject")
        summary_tag = row.select_one("div.boardListContent")
        img_tag = row.select_one("div.boardListPhoto img")

        if link_tag and title_tag:
            title = title_tag.get_text(strip=True)
            summary = summary_tag.get_text(strip=True) if summary_tag else ""
            relative_link = link_tag.get("href")
            full_link = "http://metronews.co.kr" + relative_link

            # ✅ 상세페이지에서 날짜 추출
            date = get_article_date(full_link) or datetime.today().strftime("%Y.%m.%d")

            # ✅ 썸네일 추출
            if img_tag and img_tag.get("src"):
                thumbnail = "http://metronews.co.kr" + img_tag.get("src")
            else:
                thumbnail = ""  # 기본값

            articles.append({
                "title": title,
                "summary": summary,
                "link": full_link,
                "date": date,
                "source": f"메트로뉴스 - {category}",
                "thumbnail": thumbnail  # ✅ 추가
            })

    return articles


def get_all_news():
    all_articles = []

    metro_urls = {
        "지하철": "http://metronews.co.kr/board_list.html?board_id=news&ca_name=지하철",
        "택시": "http://metronews.co.kr/board_list.html?board_id=news&ca_name=택시조합",
        "교통정책": "http://metronews.co.kr/board_list.html?board_id=news&ca_name=교통정책"
    }

    for category, url in metro_urls.items():
        all_articles.extend(get_metro_news(url, category))

    # 날짜 기준 정렬
    try:
        all_articles.sort(key=lambda x: datetime.strptime(x['date'], "%Y.%m.%d"), reverse=True)
    except:
        pass

    return all_articles

# 테스트 실행용
if __name__ == "__main__":
    from pprint import pprint
    pprint(get_all_news())
