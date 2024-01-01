import { Component } from '@angular/core';
import {SocketService} from "./socket.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  started = false;

  constructor(socket: SocketService) {
    socket.onMessage("game").subscribe(game => {
      if (!this.started) {
        this.started = true;
        setTimeout(() => socket.fireOnMessage("game", game), 20);
      }
    });
  }
}
