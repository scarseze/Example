import os
import httpx
from fastapi import FastAPI, HTTPException
from contextlib import asynccontextmanager
from pydantic import BaseModel
from qdrant_client import AsyncQdrantClient
from vector_loader import AsyncOllamaEmbedder, COLLECTION_NAME
from qdrant_client.models import Filter, FieldCondition, MatchValue
import json

# Конфигурация
QDRANT_HOST = os.getenv("QDRANT_HOST", "localhost")
QDRANT_PORT = int(os.getenv("QDRANT_PORT", 6333))
OLLAMA_HOST = os.getenv("OLLAMA_HOST", "http://localhost:11434")

# Инициализация клиентов (Lazy / Robust)
qdrant_client: AsyncQdrantClient = None
embedder: AsyncOllamaEmbedder = None
http_client: httpx.AsyncClient = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    global qdrant_client, embedder, http_client
    print(f"🚀 Starting up... Connecting to Qdrant at {QDRANT_HOST}:{QDRANT_PORT}", flush=True)
    try:
        qdrant_client = AsyncQdrantClient(host=QDRANT_HOST, port=QDRANT_PORT)
        # Verify connection
        await qdrant_client.get_collections()
        print("✅ Qdrant connected.")
    except Exception as e:
        print(f"❌ Qdrant Init Error: {e}", flush=True)
    
    print("🧠 Initializing Embedder...", flush=True)
    embedder = AsyncOllamaEmbedder(model="nomic-embed-text", base_url=OLLAMA_HOST)
    http_client = httpx.AsyncClient()
    
    yield
    
    # Shutdown
    if qdrant_client:
        await qdrant_client.close()
        print("🛑 Qdrant connection closed.")
    if http_client:
        await http_client.aclose()
        print("🛑 HTTP client closed.")

app = FastAPI(title="CBR RAG API", lifespan=lifespan)

class SearchRequest(BaseModel):
    query: str
    limit: int = 3

class SearchResponse(BaseModel):
    results: list

@app.get("/health")
def health_check():
    return {"status": "ok"}


class FinancialMetrics(BaseModel):
    Assets: float
    Equity: float
    Profit: float
    ROE: float
    Capital_Adequacy: float
    Health_Check: str

class AnalysisDetails(BaseModel):
    fact_count: int
    verdict: str

class AnalysisResponse(BaseModel):
    report_name: str
    metrics: FinancialMetrics
    details: AnalysisDetails

async def query_llm(prompt: str, model: str = "qwen2.5-coder:3b"):
    """
    Sends a prompt to Ollama to extract JSON (Async).
    """
    try:
        url = f"{OLLAMA_HOST}/api/generate"
        response = await http_client.post(url, json={
            "model": model, 
            "prompt": prompt,
            "format": "json",
            "stream": False
        })
        if response.status_code == 200:
            return response.json().get("response")
    except Exception as e:
        print(f"LLM Error: {e}")
    return None


async def analyze_bank_logic(reg_number: str):
    """
    Reusable function for Bank Analysis (RAG + LLM).
    Returns dict (metrics + details) or None if fallback needed.
    """
    print(f"Analyzing bank {reg_number}...")
    
    # 1. Retrieve Context
    try:
        client = qdrant_client # Use global initialized in lifespan
        if not client:
            print("Qdrant client not available.")
            return None

        search_filter = Filter(
            must=[FieldCondition(key="metadata.bank_regn", match=MatchValue(value=reg_number))]
        )
        records, _ = await client.scroll(
            collection_name=COLLECTION_NAME,
            scroll_filter=search_filter,
            limit=5,
            with_payload=True
        )
        
        if not records:
            print(f"No records found for reg_number {reg_number}.")
            return None

        context_text = "\n\n".join([r.payload.get("content", "") for r in records])
        
        # 2. Agentic Extraction
        prompt = f"""
        You are a financial analyst. Analyze the following text reports from the Central Bank of Russia (CBR) for a bank with RegNum {reg_number}.
        
        Extract the following metrics in JSON format:
        - Assets (Total Assets, Активы) in Billions RUB
        - Equity (Capital, Капитал) in Billions RUB
        - Profit (Прибыль) in Billions RUB
        - ROE (Return on Equity) in %
        - Capital_Adequacy (Н1.0) in % (default 12.0)
        - Health_Check (Summary string)
        
        Input Text:
        {context_text[:4000]} 
        
        Return ONLY valid JSON.
        """
        
        json_str = await query_llm(prompt)
        if json_str:
            data = json.loads(json_str)
            return {
                "report_name": f"Report (Real) {records[0].payload['metadata']['date']}",
                "metrics": {
                    "Assets": data.get("Assets", 0.0),
                    "Equity": data.get("Equity", 0.0),
                    "Profit": data.get("Profit", 0.0),
                    "ROE": data.get("ROE", 0.0),
                    "Capital_Adequacy": data.get("Capital_Adequacy", 10.0),
                    "Health_Check": data.get("Health_Check", "Analyzed")
                },
                "details": {"fact_count": len(context_text), "verdict": "Extracted via LLM"}
            }
    except Exception as e:
        print(f"RAG Error: {e}")
    
    return None

@app.get("/api/analyze")
async def get_analysis_real(reg_number: str = "1481"):
    result = await analyze_bank_logic(reg_number)
    
    if result:
        return AnalysisResponse(
            report_name=result["report_name"],
            metrics=FinancialMetrics(**result["metrics"]),
            details=AnalysisDetails(**result["details"])
        )
        
    return get_mock_response(reg_number)


def get_mock_response(reg_number):
    if reg_number == "1481":
        return AnalysisResponse(
            report_name="Sberbank 2024 (Mock)",
            metrics=FinancialMetrics(Assets=42000.5, Equity=6000.0, Profit=1200.5, ROE=20.5, Capital_Adequacy=12.5, Health_Check="Stable"),
            details=AnalysisDetails(fact_count=1500, verdict="Positive")
        )
    else:
        return AnalysisResponse(
            report_name=f"Bank {reg_number} (Mock)",
            metrics=FinancialMetrics(Assets=15000.0, Equity=2000.0, Profit=500.0, ROE=15.0, Capital_Adequacy=10.0, Health_Check="Moderate"),
            details=AnalysisDetails(fact_count=800, verdict="Neutral")
        )


# === GraphQL Implementation ===
import strawberry
from strawberry.fastapi import GraphQLRouter

@strawberry.type
class GQLFinancialMetrics:
    assets: float
    equity: float
    profit: float
    roe: float
    capital_adequacy: float
    health_check: str

@strawberry.type
class BankAnalysis:
    report_name: str
    metrics: GQLFinancialMetrics | None
    verdict: str

@strawberry.type
class Point:
    risk: float
    ret: float

@strawberry.type
class AssetMetric:
    ticker: str
    risk: float
    ret: float

@strawberry.type
class PortfolioAnalytics:
    frontier: list[Point]
    assets: list[AssetMetric]

from portfolio_analytics import get_efficient_frontier_points, get_asset_risk_return
import portfolio_analytics
print(f"LOADED portfolio_analytics from: {portfolio_analytics.__file__}", flush=True)

@strawberry.type
class Query:
    @strawberry.field
    async def bank_analysis(self, reg_number: str) -> BankAnalysis:
        # Reuse the logic
        data = await analyze_bank_logic(reg_number)
        
        if not data:
            # Fallback to mock logic just to show something
            mock = get_mock_response(reg_number)
            return BankAnalysis(
                report_name=mock.report_name,
                metrics=GQLFinancialMetrics(
                    assets=mock.metrics.Assets,
                    equity=mock.metrics.Equity,
                    profit=mock.metrics.Profit,
                    roe=mock.metrics.ROE,
                    capital_adequacy=mock.metrics.Capital_Adequacy,
                    health_check=mock.metrics.Health_Check
                ),
                verdict=mock.details.verdict
            )
            
        # Real data
        m = data["metrics"]
        return BankAnalysis(
            report_name=data["report_name"],
            metrics=GQLFinancialMetrics(
                assets=m["Assets"],
                equity=m["Equity"],
                profit=m["Profit"],
                roe=m["ROE"],
                capital_adequacy=m["Capital_Adequacy"],
                health_check=m["Health_Check"]
            ),
            verdict=data["details"]["verdict"]
        )

    @strawberry.field
    def portfolio_analytics(self, tickers: list[str]) -> PortfolioAnalytics:
        print(f"Requesting analytics for {tickers}")
        points_data = get_efficient_frontier_points(tickers)
        assets_data = get_asset_risk_return(tickers)
        
        return PortfolioAnalytics(
            frontier=[Point(risk=p["risk"], ret=p["ret"]) for p in points_data],
            assets=[AssetMetric(ticker=a["ticker"], risk=a["risk"], ret=a["ret"]) for a in assets_data]
        )


schema = strawberry.Schema(query=Query)
graphql_app = GraphQLRouter(schema)

app.include_router(graphql_app, prefix="/graphql")



# Keep existing generic search
@app.post("/search", response_model=SearchResponse)
async def search(request: SearchRequest):
    # ... existing search logic ...
    try:
        emb = embedder
        client = qdrant_client
        
        if not emb or not client:
             raise HTTPException(status_code=503, detail="Search services unavailable (Qdrant/Ollama)")

        # 1. Генерируем вектор запроса
        vector = await emb.embed_query(request.query, http_client)
        if not vector:
            raise HTTPException(status_code=500, detail="Failed to generate embedding")
        
        # 2. Ищем в Qdrant
        search_result = await client.search(
            collection_name=COLLECTION_NAME,
            query_vector=vector,
            limit=request.limit
        )
        
        # 3. Формируем ответ
        results = []
        for point in search_result:
            results.append({
                "score": point.score,
                "content": point.payload.get("content"),
                "metadata": point.payload.get("metadata")
            })
            
        return {"results": results}
        
    except Exception as e:
        print(f"Search error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
