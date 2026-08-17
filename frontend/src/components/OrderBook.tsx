import { useOrderBook } from "../hooks/useOrderBook";

export default function OrderBook() {
  const { snapshot, connected } = useOrderBook();

  return (
    <div className="orderbook">
      <div className="orderbook-header">
        <h3>Order Book</h3>
        <span className={connected ? "status-live" : "status-offline"}>
          {connected ? "\u25CF Live" : "\u25CB Connecting..."}
        </span>
      </div>

      <div className="orderbook-side asks">
        {snapshot.asks.length === 0 && <div className="orderbook-empty">No asks</div>}
        {snapshot.asks
          .slice()
          .reverse()
          .map((level, i) => (
            <div className="orderbook-row ask" key={`ask-${level.price}-${i}`}>
              <span className="price">{level.price}</span>
              <span className="qty">{level.quantity}</span>
            </div>
          ))}
      </div>

      <div className="orderbook-spread">spread</div>

      <div className="orderbook-side bids">
        {snapshot.bids.length === 0 && <div className="orderbook-empty">No bids</div>}
        {snapshot.bids.map((level, i) => (
          <div className="orderbook-row bid" key={`bid-${level.price}-${i}`}>
            <span className="price">{level.price}</span>
            <span className="qty">{level.quantity}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
