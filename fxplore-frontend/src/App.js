import React, { useState, useEffect, useCallback } from 'react';

const API_BASE = process.env.REACT_APP_API_URL || '/api';

const styles = `
  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

  :root {
    --bg:        #080b0f;
    --bg2:       #0d1117;
    --bg3:       #111820;
    --border:    #1e2a36;
    --border2:   #243040;
    --gold:      #c8952a;
    --gold2:     #e8b84b;
    --gold-dim:  #7a5a1a;
    --text:      #d4dde8;
    --text-dim:  #6b7d90;
    --text-mute: #3a4a5a;
    --green:     #2ec27e;
    --red:       #e05050;
    --blue:      #4a9eff;
    --font-head: 'Syne', sans-serif;
    --font-mono: 'DM Mono', monospace;
  }

  :root.light {
    --bg:        #f0f4f8;
    --bg2:       #ffffff;
    --bg3:       #e8edf3;
    --border:    #d0d9e4;
    --border2:   #bfcbd8;
    --gold:      #b07d1a;
    --gold2:     #c8950a;
    --gold-dim:  #e6c97a;
    --text:      #1a2433;
    --text-dim:  #4a5e72;
    --text-mute: #8fa3b8;
    --green:     #1a9e5e;
    --red:       #cc3333;
    --blue:      #2176cc;
  }

  html, body, #root { height: 100%; }

  body {
    background: linear-gradient(135deg, var(--bg) 0%, var(--bg2) 100%);
    color: var(--text);
    font-family: var(--font-mono);
    font-size: 13px;
    line-height: 1.6;
    overflow-x: hidden;
    position: relative;
  }

  body::before {
    content: '';
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: radial-gradient(circle at 20% 80%, rgba(200, 149, 42, 0.05) 0%, transparent 50%),
                radial-gradient(circle at 80% 20%, rgba(74, 158, 255, 0.05) 0%, transparent 50%);
    pointer-events: none;
    z-index: -1;
  }

  /* LAYOUT */
  .shell {
    display: grid;
    grid-template-rows: 56px 1fr;
    grid-template-columns: 220px 1fr;
    height: 100vh;
    overflow: hidden;
  }

  /* TOPBAR */
  .topbar {
    grid-column: 1 / -1;
    background: var(--bg2);
    border-bottom: 1px solid var(--border);
    display: flex;
    align-items: center;
    padding: 0 24px;
    gap: 16px;
    position: relative;
    z-index: 10;
  }

  .logo {
    font-family: var(--font-head);
    font-size: 22px;
    font-weight: 800;
    letter-spacing: -0.5px;
    color: var(--text);
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .logo-fx {
    color: var(--gold2);
  }

  .logo-dot {
    width: 6px; height: 6px;
    background: linear-gradient(45deg, var(--gold), var(--gold2));
    border-radius: 50%;
    animation: pulse 2s ease-in-out infinite;
    box-shadow: 0 0 8px var(--gold);
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; transform: scale(1); box-shadow: 0 0 8px var(--gold); }
    50% { opacity: 0.7; transform: scale(0.8); box-shadow: 0 0 16px var(--gold2); }
  }

  .topbar-ticker {
    flex: 1;
    overflow: hidden;
    mask-image: linear-gradient(to right, transparent, black 80px, black calc(100% - 80px), transparent);
  }

  .ticker-track {
    display: flex;
    gap: 32px;
    animation: ticker 30s linear infinite;
    white-space: nowrap;
    filter: drop-shadow(0 0 4px rgba(200, 149, 42, 0.3));
  }

  @keyframes ticker {
    0% { transform: translateX(0); }
    100% { transform: translateX(-50%); }
  }

  .ticker-item {
    display: flex;
    align-items: center;
    gap: 8px;
    color: var(--text-dim);
    font-size: 12px;
  }

  .ticker-pair { color: var(--text); font-weight: 500; }
  .ticker-rate { color: var(--gold2); }
  .ticker-stale { color: var(--text-mute); }

  .topbar-time {
    font-size: 12px;
    color: var(--text-dim);
    border-left: 1px solid var(--border);
    padding-left: 16px;
    font-family: var(--font-mono);
  }

  .theme-toggle {
    background: var(--bg3);
    border: 1px solid var(--border2);
    border-radius: 20px;
    padding: 5px 12px;
    cursor: pointer;
    color: var(--text-dim);
    font-family: var(--font-mono);
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 6px;
    transition: all 0.2s ease;
    white-space: nowrap;
    margin-left: 12px;
  }

  .theme-toggle:hover {
    border-color: var(--gold);
    color: var(--gold2);
    background: rgba(200,149,42,0.08);
  }

  /* SIDEBAR */
  .sidebar {
    background: var(--bg2);
    border-right: 1px solid var(--border);
    padding: 16px 0;
    overflow-y: auto;
  }

  .nav-section {
    padding: 0 12px;
    margin-bottom: 8px;
  }

  .nav-label {
    font-size: 10px;
    text-transform: uppercase;
    letter-spacing: 1.5px;
    color: var(--text-mute);
    padding: 8px 8px 6px;
  }

  .nav-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 9px 10px;
    border-radius: 8px;
    cursor: pointer;
    color: var(--text-dim);
    font-family: var(--font-mono);
    font-size: 13px;
    transition: all 0.2s ease;
    border: 1px solid transparent;
    background: none;
    width: 100%;
    text-align: left;
    position: relative;
    overflow: hidden;
  }

  .nav-item::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(200, 149, 42, 0.1), transparent);
    transition: left 0.5s;
  }

  .nav-item:hover::before {
    left: 100%;
  }

  .nav-item:hover {
    color: var(--text);
    background: var(--bg3);
    border-color: var(--border);
    transform: translateX(4px);
  }

  .nav-item.active {
    color: var(--gold2);
    background: rgba(200, 149, 42, 0.08);
    border-color: var(--gold-dim);
    box-shadow: 0 0 12px rgba(200, 149, 42, 0.2);
  }

  .nav-icon { width: 16px; text-align: center; opacity: 0.8; }

  .sidebar-footer {
    padding: 16px 20px;
    border-top: 1px solid var(--border);
    margin-top: auto;
    font-size: 11px;
    color: var(--text-mute);
  }

  /* MAIN */
  .main {
    overflow-y: auto;
    background: var(--bg);
    padding: 24px;
  }

  .page-header {
    margin-bottom: 24px;
  }

  .page-title {
    font-family: var(--font-head);
    font-size: 26px;
    font-weight: 700;
    color: var(--text);
    letter-spacing: -0.5px;
  }

  .page-sub {
    color: var(--text-dim);
    font-size: 12px;
    margin-top: 4px;
  }

  /* CARDS */
  .card {
    background: var(--bg2);
    border: 1px solid var(--border);
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    transition: all 0.3s ease;
  }

  .card:hover {
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
    transform: translateY(-2px);
  }

  .card-header {
    padding: 14px 18px;
    border-bottom: 1px solid var(--border);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .card-title {
    font-family: var(--font-head);
    font-size: 14px;
    font-weight: 600;
    color: var(--text);
    letter-spacing: 0.2px;
  }

  .card-body { padding: 18px; }

  /* GRID LAYOUTS */
  .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
  .grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
  .gap-16 { gap: 16px; }
  .mb-16 { margin-bottom: 16px; }
  .mb-24 { margin-bottom: 24px; }

  /* STAT TILES */
  .stat-tile {
    background: linear-gradient(135deg, var(--bg2) 0%, var(--bg3) 100%);
    border: 1px solid var(--border);
    border-radius: 12px;
    padding: 20px 22px;
    position: relative;
    overflow: hidden;
    transition: all 0.3s ease;
  }

  .stat-tile::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, var(--gold), var(--gold2));
  }

  .stat-tile:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.3);
  }

  .stat-label {
    font-size: 11px;
    text-transform: uppercase;
    letter-spacing: 1px;
    color: var(--text-mute);
    margin-bottom: 6px;
  }

  .stat-value {
    font-family: var(--font-head);
    font-size: 28px;
    font-weight: 700;
    color: var(--text);
    line-height: 1;
  }

  .stat-value.gold { color: var(--gold2); }
  .stat-value.green { color: var(--green); }
  .stat-value.red { color: var(--red); }

  .stat-meta { font-size: 11px; color: var(--text-mute); margin-top: 4px; }

  /* TABLE */
  .data-table {
    width: 100%;
    border-collapse: collapse;
  }

  .data-table th {
    text-align: left;
    font-size: 10px;
    text-transform: uppercase;
    letter-spacing: 1.2px;
    color: var(--text-mute);
    padding: 8px 14px;
    border-bottom: 1px solid var(--border);
    font-weight: 500;
  }

  .data-table td {
    padding: 11px 14px;
    border-bottom: 1px solid rgba(30,42,54,0.5);
    font-size: 13px;
    vertical-align: middle;
  }

  .data-table tr:last-child td { border-bottom: none; }

  .data-table tr:hover td { background: rgba(255,255,255,0.02); }

  .mono { font-family: var(--font-mono); }
  .rate-val { color: var(--gold2); font-weight: 500; }
  .pair-code { color: var(--text); font-weight: 500; font-family: var(--font-head); font-size: 14px; }

  /* BADGES */
  .badge {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 500;
    letter-spacing: 0.3px;
  }

  .badge-green { background: rgba(46,194,126,0.12); color: var(--green); }
  .badge-red { background: rgba(224,80,80,0.12); color: var(--red); }
  .badge-gold { background: rgba(200,149,42,0.12); color: var(--gold2); }
  .badge-dim { background: rgba(107,125,144,0.12); color: var(--text-dim); }

  /* FORMS */
  .form-row { display: flex; gap: 12px; align-items: flex-end; flex-wrap: wrap; }

  .form-group { display: flex; flex-direction: column; gap: 6px; flex: 1; min-width: 120px; }

  .form-label {
    font-size: 11px;
    text-transform: uppercase;
    letter-spacing: 1px;
    color: var(--text-mute);
  }

  .form-input, .form-select {
    background: var(--bg3);
    border: 1px solid var(--border2);
    border-radius: 6px;
    padding: 9px 12px;
    color: var(--text);
    font-family: var(--font-mono);
    font-size: 13px;
    outline: none;
    transition: border-color 0.15s;
    width: 100%;
  }

  .form-input:focus, .form-select:focus {
    border-color: var(--gold);
  }

  .form-select { cursor: pointer; }

  option { background: var(--bg3); }

  /* BUTTONS */
  .btn {
    padding: 10px 20px;
    border-radius: 8px;
    font-family: var(--font-mono);
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    border: none;
    transition: all 0.2s ease;
    white-space: nowrap;
    position: relative;
    overflow: hidden;
  }

  .btn::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
    transition: left 0.5s;
  }

  .btn:hover::before {
    left: 100%;
  }

  .btn-primary {
    background: linear-gradient(135deg, var(--gold), var(--gold2));
    color: #080b0f;
    box-shadow: 0 2px 8px rgba(200, 149, 42, 0.3);
  }

  .btn-primary:hover {
    background: linear-gradient(135deg, var(--gold2), var(--gold));
    box-shadow: 0 4px 16px rgba(200, 149, 42, 0.4);
    transform: translateY(-1px);
  }

  .btn-primary:disabled {
    opacity: 0.4;
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
  }

  .btn-ghost {
    background: transparent;
    border: 1px solid var(--border2);
    color: var(--text-dim);
  }

  .btn-ghost:hover { border-color: var(--border2); color: var(--text); background: var(--bg3); }

  /* RESULT BOX */
  .result-box {
    background: linear-gradient(135deg, var(--bg3) 0%, var(--bg2) 100%);
    border: 1px solid var(--border2);
    border-radius: 12px;
    padding: 24px;
    margin-top: 16px;
    animation: fadeIn 0.3s ease;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
    position: relative;
    overflow: hidden;
  }

  .result-box::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 2px;
    background: linear-gradient(90deg, var(--gold), var(--gold2), var(--gold));
  }

  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(4px); }
    to { opacity: 1; transform: translateY(0); }
  }

  .result-amount {
    font-family: var(--font-head);
    font-size: 36px;
    font-weight: 700;
    color: var(--gold2);
    line-height: 1;
  }

  .result-meta { color: var(--text-dim); font-size: 12px; margin-top: 6px; }

  /* EMPTY / LOADING / ERROR */
  .empty-state {
    padding: 40px;
    text-align: center;
    color: var(--text-mute);
    font-size: 13px;
  }

  .empty-icon { font-size: 32px; margin-bottom: 10px; opacity: 0.4; }

  .loading-dots {
    display: inline-flex;
    gap: 4px;
    align-items: center;
  }

  .loading-dots span {
    width: 5px; height: 5px;
    background: var(--gold);
    border-radius: 50%;
    animation: dot-bounce 1.2s ease-in-out infinite;
  }

  .loading-dots span:nth-child(2) { animation-delay: 0.2s; }
  .loading-dots span:nth-child(3) { animation-delay: 0.4s; }

  @keyframes dot-bounce {
    0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
    40% { transform: scale(1); opacity: 1; }
  }

  .error-msg {
    background: rgba(224,80,80,0.08);
    border: 1px solid rgba(224,80,80,0.2);
    border-radius: 6px;
    padding: 12px 16px;
    color: var(--red);
    font-size: 12px;
    margin-top: 12px;
  }

  /* DIVIDER */
  .divider { border: none; border-top: 1px solid var(--border); margin: 16px 0; }

  /* CURRENCY GRID */
  .currency-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 10px;
  }

  .currency-card {
    background: linear-gradient(135deg, var(--bg3) 0%, var(--bg2) 100%);
    border: 1px solid var(--border);
    border-radius: 10px;
    padding: 14px 16px;
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;
  }

  .currency-card::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(74, 158, 255, 0.05), transparent);
    transition: left 0.6s;
  }

  .currency-card:hover::before {
    left: 100%;
  }

  .currency-card:hover {
    border-color: var(--border2);
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  }

  .currency-iso {
    font-family: var(--font-head);
    font-size: 18px;
    font-weight: 700;
    color: var(--gold2);
  }

  .currency-name { color: var(--text-dim); font-size: 12px; margin-top: 2px; }
  .currency-symbol { color: var(--text-mute); font-size: 20px; float: right; margin-top: -24px; }

  /* STALE WARNING */
  .stale-banner {
    background: rgba(224,80,80,0.06);
    border: 1px solid rgba(224,80,80,0.15);
    border-radius: 6px;
    padding: 10px 14px;
    color: var(--red);
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
  }

  /* SCROLLBAR */
  ::-webkit-scrollbar { width: 6px; height: 6px; }
  ::-webkit-scrollbar-track { background: transparent; }
  ::-webkit-scrollbar-thumb { background: var(--border2); border-radius: 3px; }
  ::-webkit-scrollbar-thumb:hover { background: var(--text-mute); }

  /* RESPONSIVE */
  @media (max-width: 900px) {
    .shell { grid-template-columns: 1fr; grid-template-rows: 56px auto 1fr; }
    .sidebar { display: none; }
    .grid-2, .grid-3 { grid-template-columns: 1fr; }
  }
`;

// ── helpers ──────────────────────────────────────────────────────────────────

const fmt = (n, dec = 6) =>
  n != null ? Number(n).toFixed(dec) : '—';

const fmtDate = (ts) => {
  if (!ts) return '—';
  return new Date(ts).toLocaleString('en-IE', { timeZone: 'UTC', hour12: false });
};

function LoadingDots() {
  return (
    <div className="loading-dots">
      <span /><span /><span />
    </div>
  );
}

// ── API ───────────────────────────────────────────────────────────────────────

async function apiFetch(path) {
  const res = await fetch(`${API_BASE}${path}`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

// ── PAGES ─────────────────────────────────────────────────────────────────────

function Dashboard({ currencies, tickerRates }) {
  const [stale, setStale] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiFetch('/rates/stale')
      .then(setStale)
      .catch(() => setStale([]))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <div className="page-header">
        <div className="page-title">Dashboard</div>
        <div className="page-sub">Live overview — FX Rate Service</div>
      </div>

      <div className="grid-3 mb-16">
        <div className="stat-tile">
          <div className="stat-label">Active Currencies</div>
          <div className="stat-value gold">{currencies ? currencies.length : '—'}</div>
          <div className="stat-meta">in reference data</div>
        </div>
        <div className="stat-tile">
          <div className="stat-label">Tracked Pairs</div>
          <div className="stat-value">{tickerRates ? tickerRates.length : '—'}</div>
          <div className="stat-meta">with live rates</div>
        </div>
        <div className="stat-tile">
          <div className="stat-label">Stale Pairs</div>
          <div className={`stat-value ${stale && stale.length > 0 ? 'red' : 'green'}`}>
            {loading ? '…' : (stale ? stale.length : '—')}
          </div>
          <div className="stat-meta">older than 4 hours</div>
        </div>
      </div>

      {stale && stale.length > 0 && (
        <div className="stale-banner">
          ⚠ {stale.length} pair{stale.length > 1 ? 's' : ''} have stale rates (no update in 4+ hours)
        </div>
      )}

      <div className="card mb-16">
        <div className="card-header">
          <div className="card-title">Latest Rates</div>
          <span className="badge badge-gold">live</span>
        </div>
        {tickerRates && tickerRates.length > 0 ? (
          <table className="data-table">
            <thead>
              <tr>
                <th>Pair</th>
                <th>Bid</th>
                <th>Mid</th>
                <th>Ask</th>
                <th>Updated</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {tickerRates.map((r, i) => (
                <tr key={i}>
                  <td><span className="pair-code">{r.pairCode}</span></td>
                  <td className="mono rate-val">{fmt(r.bidRate)}</td>
                  <td className="mono rate-val">{fmt(r.midRate)}</td>
                  <td className="mono rate-val">{fmt(r.askRate)}</td>
                  <td className="mono" style={{ color: 'var(--text-dim)', fontSize: 11 }}>{fmtDate(r.rateTimestamp)}</td>
                  <td>
                    {r.stale
                      ? <span className="badge badge-red">stale</span>
                      : <span className="badge badge-green">live</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <div className="empty-state">
            <div className="empty-icon">📊</div>
            No rates loaded yet
          </div>
        )}
      </div>
    </div>
  );
}

function RatesLookup({ currencies }) {
  const [pairs, setPairs] = useState([
    'EUR/USD','GBP/USD','USD/JPY','USD/CHF','EUR/GBP','AUD/USD'
  ]);
  const [selectedPair, setSelectedPair] = useState('EUR/USD');
  const [customPair, setCustomPair] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [histFrom, setHistFrom] = useState('2026-01-01');
  const [histTo, setHistTo] = useState(new Date().toISOString().split('T')[0]);
  const [history, setHistory] = useState(null);
  const [histLoading, setHistLoading] = useState(false);

  const lookupRate = async () => {
    const pair = customPair.trim() || selectedPair;
    setLoading(true); setError(null); setResult(null);
    try {
      const data = await apiFetch(`/rates?pair=${encodeURIComponent(pair)}`);
      setResult(data);
    } catch (e) {
      setError(`No rate found for ${pair}`);
    } finally {
      setLoading(false);
    }
  };

  const loadHistory = async () => {
    const pair = customPair.trim() || selectedPair;
    setHistLoading(true); setHistory(null);
    try {
      const data = await apiFetch(`/rates/history?pair=${encodeURIComponent(pair)}&from=${histFrom}&to=${histTo}`);
      setHistory(data);
    } catch (e) {
      setHistory([]);
    } finally {
      setHistLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div className="page-title">Rate Lookup</div>
        <div className="page-sub">Query latest and historical rates</div>
      </div>

      <div className="card mb-16">
        <div className="card-header"><div className="card-title">Latest Rate</div></div>
        <div className="card-body">
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Pair</label>
              <select className="form-select" value={selectedPair} onChange={e => setSelectedPair(e.target.value)}>
                {pairs.map(p => <option key={p}>{p}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Or enter custom</label>
              <input className="form-input" placeholder="e.g. USD/CAD" value={customPair}
                onChange={e => setCustomPair(e.target.value.toUpperCase())} />
            </div>
            <button className="btn btn-primary" onClick={lookupRate} disabled={loading}>
              {loading ? <LoadingDots /> : 'Fetch Rate'}
            </button>
          </div>

          {error && <div className="error-msg">{error}</div>}

          {result && (
            <div className="result-box">
              <div style={{ display: 'flex', gap: 32, flexWrap: 'wrap', alignItems: 'center' }}>
                <div>
                  <div style={{ fontSize: 11, color: 'var(--text-mute)', textTransform: 'uppercase', letterSpacing: 1 }}>Pair</div>
                  <div style={{ fontFamily: 'var(--font-head)', fontSize: 22, fontWeight: 700, color: 'var(--text)' }}>{result.pairCode}</div>
                </div>
                <div>
                  <div style={{ fontSize: 11, color: 'var(--text-mute)', textTransform: 'uppercase', letterSpacing: 1 }}>Bid</div>
                  <div className="mono rate-val" style={{ fontSize: 18 }}>{fmt(result.bidRate)}</div>
                </div>
                <div>
                  <div style={{ fontSize: 11, color: 'var(--text-mute)', textTransform: 'uppercase', letterSpacing: 1 }}>Mid</div>
                  <div className="mono rate-val" style={{ fontSize: 24, color: 'var(--gold2)' }}>{fmt(result.midRate)}</div>
                </div>
                <div>
                  <div style={{ fontSize: 11, color: 'var(--text-mute)', textTransform: 'uppercase', letterSpacing: 1 }}>Ask</div>
                  <div className="mono rate-val" style={{ fontSize: 18 }}>{fmt(result.askRate)}</div>
                </div>
                <div>
                  {result.stale
                    ? <span className="badge badge-red">⚠ STALE</span>
                    : <span className="badge badge-green">● LIVE</span>}
                  <div style={{ fontSize: 11, color: 'var(--text-mute)', marginTop: 4 }}>{fmtDate(result.rateTimestamp)}</div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="card">
        <div className="card-header"><div className="card-title">Rate History</div></div>
        <div className="card-body">
          <div className="form-row mb-16">
            <div className="form-group">
              <label className="form-label">From</label>
              <input className="form-input" type="date" value={histFrom} onChange={e => setHistFrom(e.target.value)} />
            </div>
            <div className="form-group">
              <label className="form-label">To</label>
              <input className="form-input" type="date" value={histTo} onChange={e => setHistTo(e.target.value)} />
            </div>
            <button className="btn btn-primary" onClick={loadHistory} disabled={histLoading}>
              {histLoading ? <LoadingDots /> : 'Load History'}
            </button>
          </div>

          {history && history.length === 0 && (
            <div className="empty-state"><div className="empty-icon">📉</div>No records in this range</div>
          )}

          {history && history.length > 0 && (
            <table className="data-table">
              <thead>
                <tr><th>Timestamp</th><th>Bid</th><th>Mid</th><th>Ask</th><th>Status</th></tr>
              </thead>
              <tbody>
                {history.slice(0, 50).map((r, i) => (
                  <tr key={i}>
                    <td className="mono" style={{ color: 'var(--text-dim)', fontSize: 11 }}>{fmtDate(r.rateTimestamp)}</td>
                    <td className="mono">{fmt(r.bidRate)}</td>
                    <td className="mono rate-val">{fmt(r.midRate)}</td>
                    <td className="mono">{fmt(r.askRate)}</td>
                    <td>{r.stale ? <span className="badge badge-red">stale</span> : <span className="badge badge-green">live</span>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

function Converter({ currencies }) {
  const [from, setFrom] = useState('EUR');
  const [to, setTo] = useState('USD');
  const [amount, setAmount] = useState('1000');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const convert = async () => {
    setLoading(true); setError(null); setResult(null);
    try {
      const data = await apiFetch(`/convert?from=${from}&to=${to}&amount=${amount}`);
      setResult(data);
    } catch {
      setError('No rate available for this pair — direct rate and cross rates via USD &amp; EUR all unavailable');
    } finally {
      setLoading(false);
    }
  };

  const swap = () => { setFrom(to); setTo(from); setResult(null); };

  const isoCodes = currencies ? currencies.map(c => c.isoCode) : ['EUR','USD','GBP','JPY','CHF','AUD'];

  return (
    <div>
      <div className="page-header">
        <div className="page-title">Currency Converter</div>
        <div className="page-sub">Convert using latest mid rate</div>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Amount</label>
              <input className="form-input" type="number" value={amount}
                onChange={e => setAmount(e.target.value)} min="0" step="any" />
            </div>
            <div className="form-group">
              <label className="form-label">From</label>
              <select className="form-select" value={from} onChange={e => setFrom(e.target.value)}>
                {isoCodes.map(c => <option key={c}>{c}</option>)}
              </select>
            </div>
            <button className="btn btn-ghost" onClick={swap} style={{ marginBottom: 0, alignSelf: 'flex-end' }}>⇄</button>
            <div className="form-group">
              <label className="form-label">To</label>
              <select className="form-select" value={to} onChange={e => setTo(e.target.value)}>
                {isoCodes.map(c => <option key={c}>{c}</option>)}
              </select>
            </div>
            <button className="btn btn-primary" onClick={convert} disabled={loading || !amount}>
              {loading ? <LoadingDots /> : 'Convert'}
            </button>
          </div>

          {error && <div className="error-msg">{error}</div>}

          {result && (
            <div className="result-box">
              <div className="result-meta">{Number(result.amount).toLocaleString()} {result.fromCurrency} =</div>
              <div className="result-amount">
                {Number(result.convertedAmount).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 6 })} {result.toCurrency}
              </div>
              <div className="result-meta" style={{ marginTop: 10 }}>
                Rate: <span style={{ color: 'var(--gold2)' }}>{fmt(result.rate)}</span>
                &nbsp;·&nbsp; Pair: {result.pairCode}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function Fixings() {
  const [pair, setPair] = useState('EUR/USD');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const lookup = async () => {
    setLoading(true); setError(null); setResult(null);
    try {
      const data = await apiFetch(`/fixings?pair=${encodeURIComponent(pair)}&date=${date}`);
      setResult(data);
    } catch {
      setError(`No EOD fixing found for ${pair} on ${date}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div className="page-title">EOD Fixings</div>
        <div className="page-sub">Official end-of-day fixing rates</div>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Currency Pair</label>
              <input className="form-input" value={pair}
                onChange={e => setPair(e.target.value.toUpperCase())} placeholder="EUR/USD" />
            </div>
            <div className="form-group">
              <label className="form-label">Date</label>
              <input className="form-input" type="date" value={date} onChange={e => setDate(e.target.value)} />
            </div>
            <button className="btn btn-primary" onClick={lookup} disabled={loading}>
              {loading ? <LoadingDots /> : 'Lookup Fixing'}
            </button>
          </div>

          {error && <div className="error-msg">{error}</div>}

          {result && (
            <div className="result-box">
              <div style={{ display: 'flex', gap: 32, flexWrap: 'wrap' }}>
                <div>
                  <div style={{ fontSize: 11, color: 'var(--text-mute)', textTransform: 'uppercase', letterSpacing: 1 }}>Pair</div>
                  <div style={{ fontFamily: 'var(--font-head)', fontSize: 22, fontWeight: 700, color: 'var(--text)' }}>{result.pairCode}</div>
                </div>
                <div>
                  <div style={{ fontSize: 11, color: 'var(--text-mute)', textTransform: 'uppercase', letterSpacing: 1 }}>Fixing Rate</div>
                  <div className="mono" style={{ fontSize: 28, color: 'var(--gold2)', fontWeight: 600 }}>{fmt(result.fixingRate)}</div>
                </div>
                <div>
                  <div style={{ fontSize: 11, color: 'var(--text-mute)', textTransform: 'uppercase', letterSpacing: 1 }}>Date</div>
                  <div style={{ fontSize: 16, color: 'var(--text)' }}>{result.fixingDate}</div>
                </div>
                <div>
                  <div style={{ fontSize: 11, color: 'var(--text-mute)', textTransform: 'uppercase', letterSpacing: 1 }}>Official</div>
                  <div>{result.isOfficial ? <span className="badge badge-green">✓ Official</span> : <span className="badge badge-dim">Unofficial</span>}</div>
                </div>
                {result.providerCode && (
                  <div>
                    <div style={{ fontSize: 11, color: 'var(--text-mute)', textTransform: 'uppercase', letterSpacing: 1 }}>Source</div>
                    <div className="mono" style={{ color: 'var(--text-dim)' }}>{result.providerCode}</div>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function Currencies({ currencies, loading }) {
  return (
    <div>
      <div className="page-header">
        <div className="page-title">Currencies</div>
        <div className="page-sub">All active currencies in reference data</div>
      </div>

      <div className="card">
        <div className="card-header">
          <div className="card-title">Active Currencies</div>
          <span className="badge badge-gold">{currencies ? currencies.length : '—'} total</span>
        </div>
        <div className="card-body">
          {loading && <div className="empty-state"><LoadingDots /></div>}
          {!loading && currencies && (
            <div className="currency-grid">
              {currencies.map((c, i) => (
                <div className="currency-card" key={i}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div className="currency-iso">{c.isoCode}</div>
                    <div style={{ fontSize: 20, opacity: 0.5 }}>{c.symbol || ''}</div>
                  </div>
                  <div className="currency-name">{c.currencyName || c.name || '—'}</div>
                  {c.countryCode && (
                    <div style={{ fontSize: 11, color: 'var(--text-mute)', marginTop: 4 }}>{c.countryCode}</div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function StoreRate() {
  const [form, setForm] = useState({ pairCode: 'EUR/USD', providerCode: 'ECB', bid: '', ask: '', mid: '' });
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(null);
  const [error, setError] = useState(null);

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const submit = async () => {
    setLoading(true); setError(null); setSuccess(null);
    try {
      const res = await fetch(`${API_BASE}/rates`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          pairCode: form.pairCode,
          providerCode: form.providerCode,
          bid: parseFloat(form.bid),
          ask: parseFloat(form.ask),
          mid: parseFloat(form.mid),
        }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || `HTTP ${res.status}`);
      }
      const data = await res.json();
      setSuccess(data.message);
      setForm(f => ({ ...f, bid: '', ask: '', mid: '' }));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div className="page-title">Store Rate</div>
        <div className="page-sub">Manually push a new exchange rate</div>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="grid-2 mb-16">
            <div className="form-group">
              <label className="form-label">Currency Pair</label>
              <input className="form-input" value={form.pairCode}
                onChange={e => set('pairCode', e.target.value.toUpperCase())} placeholder="EUR/USD" />
            </div>
            <div className="form-group">
              <label className="form-label">Provider Code</label>
              <input className="form-input" value={form.providerCode}
                onChange={e => set('providerCode', e.target.value.toUpperCase())} placeholder="ECB" />
            </div>
          </div>
          <div className="grid-3 mb-16">
            <div className="form-group">
              <label className="form-label">Bid</label>
              <input className="form-input" type="number" step="any" value={form.bid}
                onChange={e => set('bid', e.target.value)} placeholder="1.082000" />
            </div>
            <div className="form-group">
              <label className="form-label">Mid</label>
              <input className="form-input" type="number" step="any" value={form.mid}
                onChange={e => set('mid', e.target.value)} placeholder="1.083000" />
            </div>
            <div className="form-group">
              <label className="form-label">Ask</label>
              <input className="form-input" type="number" step="any" value={form.ask}
                onChange={e => set('ask', e.target.value)} placeholder="1.084000" />
            </div>
          </div>

          <div style={{ fontSize: 11, color: 'var(--text-mute)', marginBottom: 16 }}>
            Required: bid &lt; mid &lt; ask and all values must be positive
          </div>

          <button className="btn btn-primary" onClick={submit}
            disabled={loading || !form.bid || !form.ask || !form.mid}>
            {loading ? <LoadingDots /> : 'Store Rate'}
          </button>

          {success && (
            <div style={{ marginTop: 12, background: 'rgba(46,194,126,0.08)', border: '1px solid rgba(46,194,126,0.2)', borderRadius: 6, padding: '10px 14px', color: 'var(--green)', fontSize: 12 }}>
              ✓ {success}
            </div>
          )}
          {error && <div className="error-msg">✗ {error}</div>}
        </div>
      </div>
    </div>
  );
}

// ── MAIN APP ──────────────────────────────────────────────────────────────────

const NAV = [
  { id: 'dashboard',  label: 'Dashboard',   icon: '◈' },
  { id: 'rates',      label: 'Rates',        icon: '◎' },
  { id: 'converter',  label: 'Converter',    icon: '⇄' },
  { id: 'fixings',    label: 'EOD Fixings',  icon: '◷' },
  { id: 'currencies', label: 'Currencies',   icon: '◉' },
  { id: 'store',      label: 'Store Rate',   icon: '＋' },
];

const QUICK_PAIRS = ['EUR/USD','GBP/USD','USD/JPY','EUR/GBP','USD/CHF','AUD/USD'];

export default function App() {
  const [page, setPage] = useState('dashboard');
  const [currencies, setCurrencies] = useState(null);
  const [currLoading, setCurrLoading] = useState(true);
  const [tickerRates, setTickerRates] = useState([]);
  const [now, setNow] = useState(new Date());
  const [darkMode, setDarkMode] = useState(true);

  useEffect(() => {
    document.documentElement.classList.toggle('light', !darkMode);
  }, [darkMode]);

  useEffect(() => {
    apiFetch('/currencies')
      .then(setCurrencies)
      .catch(() => setCurrencies([]))
      .finally(() => setCurrLoading(false));
  }, []);

  // Load ticker rates
  useEffect(() => {
    const load = async () => {
      const results = await Promise.allSettled(
        QUICK_PAIRS.map(p => apiFetch(`/rates?pair=${encodeURIComponent(p)}`))
      );
      setTickerRates(results.filter(r => r.status === 'fulfilled').map(r => r.value));
    };
    load();
    const id = setInterval(load, 30000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  const tickerDouble = [...tickerRates, ...tickerRates];

  return (
    <>
      <style>{styles}</style>
      <div className="shell">
        {/* TOPBAR */}
        <header className="topbar">
          <div className="logo">
            <span className="logo-fx">FX</span>plore
            <div className="logo-dot" />
          </div>
          <div className="topbar-ticker">
            {tickerDouble.length > 0 && (
              <div className="ticker-track">
                {tickerDouble.map((r, i) => (
                  <div className="ticker-item" key={i}>
                    <span className="ticker-pair">{r.pairCode}</span>
                    <span className="ticker-rate">{fmt(r.midRate, 5)}</span>
                    {r.stale && <span className="ticker-stale">●</span>}
                  </div>
                ))}
              </div>
            )}
          </div>
          <div className="topbar-time">
            UTC {now.toISOString().slice(11, 19)}
          </div>
          <button className="theme-toggle" onClick={() => setDarkMode(d => !d)}>
            {darkMode ? '☀ Light' : '◑ Dark'}
          </button>
        </header>

        {/* SIDEBAR */}
        <nav className="sidebar">
          <div className="nav-section">
            <div className="nav-label">Navigation</div>
            {NAV.map(n => (
              <button key={n.id} className={`nav-item ${page === n.id ? 'active' : ''}`}
                onClick={() => setPage(n.id)}>
                <span className="nav-icon">{n.icon}</span>
                {n.label}
              </button>
            ))}
          </div>
          <div style={{ flex: 1 }} />
          <div className="sidebar-footer">
            FXplore v1.0<br />
            Team 3 · GSE Final Project
          </div>
        </nav>

        {/* MAIN */}
        <main className="main">
          {page === 'dashboard'  && <Dashboard currencies={currencies} tickerRates={tickerRates} />}
          {page === 'rates'      && <RatesLookup currencies={currencies} />}
          {page === 'converter'  && <Converter currencies={currencies} />}
          {page === 'fixings'    && <Fixings />}
          {page === 'currencies' && <Currencies currencies={currencies} loading={currLoading} />}
          {page === 'store'      && <StoreRate />}
        </main>
      </div>
    </>
  );
}