"""配置中心 — 从 .env 加载，支持 LLM / Embedding 分离配置。

DeepSeek 不提供 embedding API，embedding 需单独配置提供商（如 OpenAI）。
- 已配置 embedding → ES kNN 语义搜索
- 未配置 embedding → ES multi_match 文本搜索（自动降级）
"""
import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv(Path(__file__).resolve().parent.parent / ".env", override=True)


class Settings:
    # ====== LLM 对话模型 ======
    llm_api_key: str = os.getenv("LLM_API_KEY", "")
    llm_base_url: str = os.getenv("LLM_BASE_URL", "https://api.deepseek.com")
    llm_model: str = os.getenv("LLM_MODEL", "deepseek-chat")

    # ====== Embedding 模型（独立配置，不 fallback 到 LLM） ======
    embedding_api_key: str = os.getenv("EMBEDDING_API_KEY", "")
    embedding_base_url: str = os.getenv("EMBEDDING_BASE_URL", "")
    embedding_model: str = os.getenv("EMBEDDING_MODEL", "")
    embedding_dim: int = int(os.getenv("EMBEDDING_DIM", "2560"))

    # ====== Elasticsearch ======
    es_host: str = os.getenv("ES_HOST", "http://localhost:9200")
    es_index: str = os.getenv("ES_INDEX", "blog_articles")
    kb_index: str = os.getenv("KB_INDEX", "kb_chunks")

    # ====== MySQL（知识库事实源） ======
    mysql_host: str = os.getenv("MYSQL_HOST", "localhost")
    mysql_port: int = int(os.getenv("MYSQL_PORT", "3306"))
    mysql_user: str = os.getenv("MYSQL_USER", "root")
    mysql_password: str = os.getenv("MYSQL_PASSWORD", "123456")
    mysql_db: str = os.getenv("MYSQL_DB", "blog2026")

    # ====== Chat ======
    chat_max_tokens: int = int(os.getenv("CHAT_MAX_TOKENS", "2048"))
    chat_temperature: float = float(os.getenv("CHAT_TEMPERATURE", "0.7"))
    retrieval_top_k: int = int(os.getenv("RETRIEVAL_TOP_K", "5"))
    internal_token: str = os.getenv("CHAT_ASSISTANT_TOKEN", "")

    def validate(self) -> list[str]:
        """启动时校验必要配置，返回错误列表（空列表 = 全部 OK）。"""
        errors = []
        if not self.llm_api_key:
            errors.append("LLM_API_KEY 未设置")
        if not self.llm_base_url:
            errors.append("LLM_BASE_URL 未设置")
        if not self.llm_model:
            errors.append("LLM_MODEL 未设置")
        # embedding 是可选的，不完整配置不阻止启动（EmbeddingService 自动降级）
        if bool(self.embedding_api_key) != bool(self.embedding_base_url):
            pass  # 静默降级，由 /health 端点告知状态
        return errors


settings = Settings()
