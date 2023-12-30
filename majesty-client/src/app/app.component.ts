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
    socket.onMessage("game").subscribe(() => {
      this.started = true;
    });
  }
}
