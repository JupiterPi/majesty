import {Injectable} from "@angular/core";
import {Observable, Subject} from "rxjs";
import {environment} from "../environments/environment";

export interface Request {
  requestId: string;
  type: string;
  payload: string | null;
}

@Injectable({
  providedIn: "root"
})
export class SocketService {
  private ws?: WebSocket;
  private topics = new Map<string, Subject<any>>();

  connect(gameId: string, player: string, onConnect: () => void) {
    this.ws = new WebSocket(`ws://${environment.host}/game/${gameId}/game/${player}`);
    this.ws.addEventListener("message", (message: MessageEvent) => {
      const packet = JSON.parse(message.data) as {topic: string, payload: any};
      const subject = this.topics.get(packet.topic);
      if (subject != undefined) subject.next(packet.payload);
      else console.error("No listener for packet: ", packet);
    });
    this.ws.addEventListener("open", () => onConnect());
    this.ws.addEventListener("error", err => console.error(err));
    this.ws.addEventListener("close", event => {
      if (event.code != 1000) console.error("WebSocket connection closed:", event.code, event.reason);
    });
  }

  onMessage(topic: string): Observable<any> {
    const subject = this.topics.get(topic);
    if (subject == undefined) {
      const newSubject = new Subject<any>();
      this.topics.set(topic, newSubject);
      return newSubject;
    } else {
      return subject;
    }
  }
  fireOnMessage(topic: string, payload: any) {
    this.topics.get(topic)?.next(payload);
  }

  sendMessage(topic: string, payload: any) {
    const message = JSON.stringify({topic, payload});
    this.ws!.send(message);
  }
}
