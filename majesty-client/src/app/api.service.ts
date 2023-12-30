import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {environment} from "../environments/environment";

@Injectable({
  providedIn: "root"
})
export class ApiService {
  constructor(private http: HttpClient) {}

  joinGame(gameId: string, name: string) {
    return this.http.post(`${environment.root}/game/${gameId}/join`, {name}, {responseType: "text"});
  }

  startGame(gameId: string) {
    return this.http.post(`${environment.root}/game/${gameId}/start`, null, {responseType: "text"});
  }
}
