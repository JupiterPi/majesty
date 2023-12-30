import {Component} from '@angular/core';
import {ApiService} from "../api.service";
import {SocketService} from "../socket.service";

@Component({
  selector: 'app-lobby',
  templateUrl: './lobby.component.html',
  styleUrls: ['./lobby.component.scss']
})
export class LobbyComponent {
  playerName = "Bob";
  gameId = "";
  joined = false;

  constructor(private api: ApiService, private socket: SocketService) {}

  join() {
    if (this.gameId == "") return;
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
