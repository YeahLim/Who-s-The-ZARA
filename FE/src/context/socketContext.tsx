import { createContext, useContext, useEffect, useState } from "react";
import { LayoutChildrenProps } from "../types/LayoutChildrenProps";
import { Client } from "@stomp/stompjs";
import { useAccessTokenState } from "./accessTokenContext";
import { stompBaseUrl } from "../api/url/baseUrl";

interface WebSocketContextProps {
  client: Client | undefined;
}

const WebSocketContext = createContext<WebSocketContextProps>({
  client: new Client(),
});

export const WebSocketProvider = ({ children }: LayoutChildrenProps) => {
  const [client, setClient] = useState<Client | undefined>(undefined);
  const { accessToken } = useAccessTokenState();

  useEffect(() => {
    if (!accessToken) return;
    const newClient = new Client({
      brokerURL: stompBaseUrl,
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      beforeConnect: () => {
        console.log("Connecting to WebSocket");
      },
      onConnect: () => {
        console.log("Connected to WebSocket");
      },
      onDisconnect: () => {
        console.log("Disconnected from WebSocket");
      },
      onWebSocketError: (error) => {
        console.log("WebSocket error: ", error);
      },
    });
    setClient(newClient);
    newClient.activate();
  }, [accessToken]);

  return <WebSocketContext.Provider value={{ client }}>{children}</WebSocketContext.Provider>;
};

export const useWebSocket = (): WebSocketContextProps => {
  const context = useContext(WebSocketContext);
  if (context === null) {
    throw new Error("useWebSocket must be used within a WebSocketProvider");
  }
  return context;
};
