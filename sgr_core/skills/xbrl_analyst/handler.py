import os
from typing import Type
import pandas as pd
from pydantic import BaseModel
try:
    from lxml import etree
except ImportError:
    etree = None

from core.state import AgentState
from skills.base import BaseSkill
from skills.xbrl_analyst.schema import XBRLInput

class XBRLAnalystSkill(BaseSkill):
    name: str = "xbrl_analyst"
    description: str = (
        "Parses XBRL financial reports (XML format ONLY) to extract key metrics. "
        "DO NOT use for PDF, Markdown, or plain text analysis."
    )
    
    def __init__(self, llm_service=None):
        self._llm = llm_service

    @property
    def input_schema(self) -> Type[BaseModel]:
        return XBRLInput

    async def execute(self, params: XBRLInput, state: AgentState) -> str:
        if etree is None:
            return "Error: 'lxml' library is missing. Cannot parse XBRL."
            
        # Resolve path (handle relative paths)
        file_path = params.file_path
        # Look in known data dirs if file not found directly
        if not os.path.exists(file_path):
            potential_paths = [
                os.path.join(os.getcwd(), "data", file_path),
                os.path.join(os.getcwd(), file_path)
            ]
            for p in potential_paths:
                if os.path.exists(p):
                    file_path = p
                    break
        
        if not os.path.exists(file_path):
            return f"Error: File not found at {file_path}"
            
        try:
            facts = self._parse_xbrl_logic(file_path)
            metrics = self._calculate_metrics_logic(facts)
            
            # Format Output
            output = [f"### XBRL Analysis: {os.path.basename(file_path)}"]
            
            # Health Check
            status = metrics.get('Health Check', 'UNKNOWN')
            icon = "✅" if status == "OK" else "⚠️"
            output.append(f"**Health Status**: {icon} {status}\n")
            
            output.append("| Metric | Value |")
            output.append("|---|---|")
            for k, v in metrics.items():
                if k != 'Health Check':
                    # Format numbers nicely
                    val_str = f"{v:,.2f}" if isinstance(v, (int, float)) else str(v)
                    output.append(f"| {k} | {val_str} |")
            
            if params.extract_all:
                output.append(f"\n*(Total Raw Facts Extracted: {len(facts)})*")
                
            return "\n".join(output)

        except Exception as e:
            return f"XBRL Parsing Failed: {str(e)}"

    def _parse_xbrl_logic(self, file_path):
        """Core parsing logic ported from parser.py"""
        tree = etree.parse(file_path)
        root = tree.getroot()
        
        contexts = {}
        for context in root.findall('.//{http://www.xbrl.org/2003/instance}context'):
            ctx_id = context.get('id')
            period = context.find('{http://www.xbrl.org/2003/instance}period')
            instant = period.find('{http://www.xbrl.org/2003/instance}instant')
            
            if instant is not None:
                contexts[ctx_id] = {'type': 'instant', 'date': instant.text}
            # Simplified context logic for brevity
        
        facts = []
        for child in root:
            context_ref = child.get('contextRef')
            if not context_ref: continue
            
            # Simple localname extraction
            tag = child.tag
            concept = tag.split('}')[-1] if '}' in tag else tag
            
            facts.append({
                'concept': concept,
                'value': child.text,
                'unit': child.get('unitRef')
            })
            
        return facts

    def _calculate_metrics_logic(self, facts):
        """Core metric calculation ported from parser.py"""
        df = pd.DataFrame(facts)
        
        def get_val(concept_name):
            if df.empty: return 0.0
            row = df[df['concept'] == concept_name]
            if row.empty: return 0.0
            try: return float(row.iloc[0]['value'])
            except: return 0.0

        assets = get_val('Assets')
        equity = get_val('Equity') # Assuming simple mapping for now
        profit = get_val('NetProfit')
        
        # Determine specific concept names based on common taxonomies (or mock data)
        # If headers are different in real files, we'll need to update this map
        if assets == 0: assets = get_val('AssetsTotal') # Try alias
        
        metrics = {
            'Assets': assets,
            'Equity': equity,
            'Net Profit': profit,
            'ROE': round((profit / equity * 100), 2) if equity else 0.0,
            'Capital Adequacy': round((equity / assets * 100), 2) if assets else 0.0,
            'Health Check': 'OK' if (assets > 0 and (equity / assets) > 0.08) else 'RISK'
        }
        return metrics
