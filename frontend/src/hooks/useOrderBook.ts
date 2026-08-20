import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import type { IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";

export interface OrderBookLevel {
  price: string;
  quantity: string;
}

export interface OrderBookSnapshot {
  bids: OrderBookLevel[];
  asks: OrderBookLevel[];
}

const WS_URL = "http://localhost:8080/ws";

export function useOrderBook() {
  const [snapshot, setSnapshot] = useState<OrderBookSnapshot>({ bids: [], asks: [] });
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        client.subscribe("/topic/orderbook", (message: IMessage) => {
          const data: OrderBookSnapshot = JSON.parse(message.body);
          setSnapshot(data);
        });
      },
      onDisconnect: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, []);

  return { snapshot, connected };
}
