import {Component} from '@angular/core';
import {Card, Game, Place, Player} from "../data";
import {Request, SocketService} from "../socket.service";
import {AuthService} from "../auth.service";

@Component({
  selector: 'app-game',
  templateUrl: './game.component.html',
  styleUrls: ['./game.component.scss']
})
export class GameComponent {
  game?: Game;

  constructor(private socket: SocketService, private auth: AuthService) {
    socket.onMessage("game").subscribe(game => this.game = game);
    socket.onMessage("request").subscribe((request: Request) => {
      this.currentRequestId = request.requestId;
      const requestPayload = request.payload != null ? JSON.parse(request.payload) : null;
      if (request.type == "card_from_queue") {
        this.queueSelectable = true;
      }
      if (request.type == "healed_card_place") {
        this.placesSelectable = (requestPayload as Card).places;
      }
    });
  }
  currentRequestId?: string;
  sendResponse(payload: any) {
    this.socket.sendMessage("request", {requestId: this.currentRequestId, payload: JSON.stringify(payload)});
  }

  selfPlayer() {
    return this.game!.players.filter(player => player.name == this.auth.playerName)[0];
  }
  otherPlayers() {
    return this.game!.players.filter(player => player.name != this.auth.playerName);
  }

  cards(player: Player) {
    return Object.entries(player.cards) as [Place, Card[]][];
  }
  isLeadingPlayer(player: Player) {
    return player == this.game!.players.slice().sort((a, b) => b.score - a.score)[0];
  }

  queueSelectable = false;
  isCardInQueueSelectable(index: number, place: Place) {
    if (!this.queueSelectable) return false;
    if (index != this.game!.cardsQueue.findIndex(card => card.card.places.includes(place))) return false;
    if (index > this.selfPlayer().meeples) return false;
    return true;
  }
  selectCardInQueue(index: number, place: Place) {
    if (!this.isCardInQueueSelectable(index, place)) return;
    this.sendResponse({place});
    this.queueSelectable = false;
  }

  placesSelectable: Place[] = [];
  isPlaceSelectable(place: Place) {
    return this.placesSelectable.includes(place);
  }
  selectPlace(place: Place) {
    if (!this.isPlaceSelectable(place)) return;
    this.sendResponse({place});
    this.placesSelectable = [];
  }
}
