import {Component} from '@angular/core';
import {ApiService} from "../api.service";
import {SocketService} from "../socket.service";
import {CookieService} from "ngx-cookie";
import {AuthService} from "../auth.service";
import {ActivatedRoute, Router} from "@angular/router";
import {environment} from "../../environments/environment";

@Component({
  selector: 'app-lobby',
  templateUrl: './lobby.component.html',
  styleUrls: ['./lobby.component.scss']
})
export class LobbyComponent {
  playerName = this.cookies.get("name") ?? "";
  gameId = "";
  joined = false;
  lobbyPlayers: string[] = [];

  constructor(
    private cookies: CookieService,
    private api: ApiService,
    private socket: SocketService,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
  ) {
    route.queryParams.subscribe(queryParams => {
      this.gameId = queryParams["game"] ?? "";
    });
  }

  createGame() {
    this.api.createGame().subscribe(id => this.gameId = id);
  }

  join() {
    if (this.playerName == "" || this.gameId == "") return;
    this.cookies.put("name", this.playerName);
    this.auth.playerName = this.playerName;
    this.router.navigate([], {relativeTo: this.route, queryParams: {"game": this.gameId}});
    this.api.joinGame(this.gameId, this.playerName).subscribe(() => {
      this.socket.connect(this.gameId, this.playerName, () => {
        this.joined = true;
        this.socket.onMessage("lobby_players").subscribe((players: {players: string[]}) => {
          this.lobbyPlayers = players.players;
        });
      });
    });
  }

  leave() {
    this.api.leaveGame(this.gameId, this.playerName).subscribe();
    window.location.href = "";
  }

  copied = false;
  copyLink() {
    const path = this.router.createUrlTree([], {relativeTo: this.route, queryParams: {"game": this.gameId}}).toString();
    navigator.clipboard.writeText(environment.clientRoot + path);
    this.copied = true;
    setTimeout(() => this.copied = false, 3000);
  }

  startGame() {
    if (!this.joined) return;
    this.api.startGame(this.gameId).subscribe();
  }
}
