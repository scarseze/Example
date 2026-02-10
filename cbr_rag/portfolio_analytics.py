import okama as ok
import pandas as pd
import traceback
from typing import List, Dict

def validate_tickers(tickers: List[str], ccy: str) -> List[str]:
    """Filter out tickers that don't have enough history for Okama (return None risk)."""
    valid_tickers = []
    try:
        # Check individually or in small batches? 
        # AssetList is fast enough usually.
        al = ok.AssetList(tickers, ccy=ccy)
        risk = al.risk_annual
        
        for t in tickers:
            try:
                val = risk[t]
                if hasattr(val, 'iloc'): val = val.iloc[-1]
                
                if val is not None and not pd.isna(val):
                    valid_tickers.append(t)
                else:
                    print(f"Skipping {t}: No risk data (short history?)")
            except:
                print(f"Skipping {t}: Error accessing risk")
                
        return valid_tickers
    except Exception as e:
        print(f"Validation failed: {e}")
        return tickers # Fallback to trying all

def get_efficient_frontier_points(tickers: List[str], ccy: str = "RUB") -> List[Dict[str, float]]:
    """
    Calculates Efficient Frontier for a given list of tickers.
    Returns a list of dicts: [{"risk": 0.1, "return": 0.15}, ...]
    """
    try:
        print(f"Original tickers: {tickers}")
        valid_tickers = validate_tickers(tickers, ccy)
        print(f"Valid tickers for EF: {valid_tickers}")
        
        if len(valid_tickers) < 2:
            print("Not enough valid tickers for EF (<2).")
            return []

        print(f"Calculating Efficient Frontier for {valid_tickers} in {ccy} (from Okama)...")
        # Omit last_date to use default (latest available) which is safer than 'last' string literal
        ef = ok.EfficientFrontier(valid_tickers, ccy=ccy) 
        
        
        # Get 20 points on the EF
        # ef_points returns a DataFrame with 'Risk' (volatility) and 'CNR' (Compound Annual Return) usually or 'Mean return' depending on settings
        # okama default ef_points uses CAGR (Geometric mean) and Risk (std dev)
        df = ef.ef_points
        
        points = []
        for risk, ret in zip(df['Risk'], df['CAGR']):
            points.append({
                "risk": float(risk),
                "ret": float(ret) # 'return' is reserved keyword
            })
            
        return points
    except Exception as e:
        print(f"Okama Review Error: {e}")
        traceback.print_exc()
        return []

def get_asset_risk_return(tickers: List[str], ccy: str = "RUB") -> List[Dict[str, float]]:
    """
    Get Risk/Return for individual assets.
    """
    try:
        # For asset list, we can show even bad tickers (as 0.0), so we use original list
        al = ok.AssetList(tickers, ccy=ccy)
        risk = al.risk_annual
        # cagr is a method, not a property
        cagr = al.get_cagr()
        
        
        results = []
        for t in tickers:
            # Helper to extract scalar from potentially rolling series
            # Helper to extract scalar from potentially rolling series/dataframe/float
            r_source = risk[t]
            r_val = 0.0
            
            # If it's a Series (e.g. column from DF), take last value
            if hasattr(r_source, 'iloc'):
                r_val = r_source.iloc[-1]
            else:
                r_val = r_source
            
            # Handle None/NaN
            if r_val is None or pd.isna(r_val): 
                r_val = 0.0
            r_val = float(r_val)
            
            c_source = cagr[t]
            c_val = 0.0
             # If it's a Series or DF, take last value. If scalar, just use it.
            if hasattr(c_source, 'iloc'):
                c_val = c_source.iloc[-1]
            else:
                c_val = c_source
                
            # Handle None/NaN
            if c_val is None or pd.isna(c_val):
                c_val = 0.0
            c_val = float(c_val)

            print(f"DEBUG {t}: Risk={r_val}, Ret={c_val}")

            results.append({
                "ticker": t,
                "risk": float(r_val),
                "ret": float(c_val)
            })
        return results
    except Exception as e:
        print(f"Okama Asset Error: {e}")
        traceback.print_exc()
        return []
