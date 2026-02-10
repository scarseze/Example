import okama as ok
import sys

from typing import List

# 1. Список "Голубых фишек" и основных индексов
# Можно расширить до полного состава IMOEX
MOEX_TICKERS = [
    'SBER.MOEX', 'GAZP.MOEX', 'LKOH.MOEX', 'GMKN.MOEX', 
    'NVTK.MOEX', 'YDEX.MOEX', 'ROSN.MOEX', 'TATN.MOEX', 
    'PLZL.MOEX', 'SNGS.MOEX', 'MGNT.MOEX', 'MTSS.MOEX',
    'MOEX.MOEX', 'ALRS.MOEX', 'NLMK.MOEX', 'CHMF.MOEX',
    'RGBITR.INDX', 'IMOEX.INDX', 'USDRUB.FX', 'EURRUB.FX'
]

def warmup_cache(tickers: List[str] = MOEX_TICKERS):
    print("🚀 Starting MOEX Data Warmup...")
    
    try:
        # 2. Создаем AssetList. 
        # Okama автоматически скачивает данные при инициализации или первом обращении
        print("📥 Downloading history (default/max)...")
        # Removing explicit dates allow okama to fetch latest available
        al = ok.AssetList(tickers, ccy='RUB')
        
        # 3. Триггерим вычисления, чтобы убедиться, что данные скачались и распарсились
        _ = al.wealth_indexes
        _ = al.risk_annual
        
        print("\n✅ Success! Data cached locally.")
        print(f"Loaded {al.symbols}")
        print("Sample Data (Last Cumulative Return):")
        print(al.wealth_indexes.iloc[-1])
        
    except Exception as e:
        print(f"\n❌ Error during warmup: {e}")
        sys.exit(1)

if __name__ == "__main__":
    warmup_cache()
