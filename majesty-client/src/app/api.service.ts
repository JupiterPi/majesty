import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {environment} from "../environments/environment";
import {map} from "rxjs";

@Injectable({
  providedIn: "root"
})
export class ApiService {
  constructor(private http: HttpClient) {}

  createGame() {
    return this.http.get<{id: string}>(`${environment.apiRoot}/api/newGame`).pipe(map(dto => dto.id));
  }

  joinGame(gameId: string, name: string) {
    return this.http.post(`${environment.apiRoot}/api/game/${gameId}/join`, {name}, {responseType: "text"});
  }

  leaveGame(gameId: string, name: string) {
    return this.http.post(`${environment.apiRoot}/api/game/${gameId}/leave`, {name}, {responseType: "text"});
  }

  startGame(gameId: string) {
    return this.http.post(`${environment.apiRoot}/api/game/${gameId}/start`, null, {responseType: "text"});
  }
}
