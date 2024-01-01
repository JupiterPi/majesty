import {Component} from '@angular/core';
import {ApiService} from "../api.service";
import {SocketService} from "../socket.service";
import {CookieService} from "ngx-cookie";
import {AuthService} from "../auth.service";

@Component({
  selector: 'app-lobby',
  templateUrl: './lobby.component.html',
  styleUrls: ['./lobby.component.scss']
})
export class LobbyComponent {
  playerName = this.cookies.get("name") ?? "";
  gameId = "";
  joined = false;

  constructor(private cookies: CookieService, private api: ApiService, private socket: SocketService, private auth: AuthService) {}

  join() {
    if (this.playerName == "" || this.gameId == "") return;
    this.cookies.put("name", this.playerName);
    this.auth.playerName = this.playerName;
    this.api.joinGame(this.gameId, this.playerName).subscribe(() => {
      this.socket.connect(this.gameId, this.playerName, () => {
        this.joined = true;
      });
    });
  }

  startGame() {
    if (!this.joined) return;
    this.api.startGame(this.gameId).subscribe();
  }
}
