import requests
from bs4 import BeautifulSoup

def get_popular_news():
    url = "http://metronews.co.kr/board_list.html?board_id=news&ca_name=%EC%A7%80%ED%95%98%EC%B2%A0"
    response = requests.get(url)
    response.encoding = "utf-8"
    soup = BeautifulSoup(response.text, "html.parser")

    popular = []
    # 인기기사 영역을 찾음 (ul 태그 안의 li들 기준)
    for li in soup.select(".rank_best li")[:5]:
        title_tag = li.select_one("a")
        if title_tag:
            popular.append({
                "title": title_tag.text.strip(),
                "link": "http://metronews.co.kr" + title_tag["href"]
            })

    return popular
