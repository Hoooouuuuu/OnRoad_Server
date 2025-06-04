from flask import Flask
from flask_cors import CORS
from routes.chart_routes import bp as chart_bp
from routes.news_routes import bp as news_bp

app = Flask(__name__)
CORS(app)  # ✅ 모든 도메인 허용. 보안 강화 시 origins 지정 가능

app.register_blueprint(chart_bp)
app.register_blueprint(news_bp)

if __name__ == "__main__":
    app.run(debug=True, port=5001)
