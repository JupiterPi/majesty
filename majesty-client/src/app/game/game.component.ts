import {Component, ElementRef, ViewChild} from '@angular/core';
import {
  barracksPlayedNotification,
  breweryPlayedNotification,
  Card,
  cardTakenNotification,
  castlePlayedNotification,
  cottagePlayedNotification,
  finalScoringNotification,
  Game, GameEndNotification,
  guardhousePlayedNotification,
  innPlayedNotification,
  meeplesSoldNotification,
  messageNotification,
  millPlayedNotification,
  Notification,
  Place,
  Player,
  gameEndNotification,
} from "../data";
import {Request, SocketService} from "../socket.service";
import {AuthService} from "../auth.service";

@Component({
  selector: 'app-game',
  templateUrl: './game.component.html',
  styleUrls: ['./game.component.scss']
})
export class GameComponent {
  game?: Game;
  notifications: Notification[] = [];
  gameResult?: GameEndNotification;

  @ViewChild("chatMessagesContainer") chatMessagesContainer!: ElementRef<HTMLDivElement>;

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
    socket.onMessage("notification").subscribe((notification: Notification) => {
      if (notification.type == "game_end") {
        this.gameResult = notification.notification as GameEndNotification;
      }
      this.notifications.push(notification);
      setTimeout(() => {
        this.chatMessagesContainer.nativeElement.scrollTop = this.chatMessagesContainer.nativeElement.scrollHeight;
      }, 100);
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

  queueSelectable = false;
  isCardInQueueSelectable(index: number) {
    return this.queueSelectable && index <= this.selfPlayer().meeples;
  }
  selectCardInQueue(index: number, place: Place) {
    if (!this.isCardInQueueSelectable(index)) return;
    this.sendResponse({cardIndex: index, place});
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

  chatInput = "";
  sendChatMessage() {
    if (this.chatInput == "") return;
    this.socket.sendMessage("chat", {message: this.chatInput});
    this.chatInput = "";
  }

  joinRematch(rematchId: string) {
    window.location.href = "/?game=" + rematchId;
  }

  protected readonly messageNotification = messageNotification;
  protected readonly millPlayedNotification = millPlayedNotification;
  protected readonly breweryPlayedNotification = breweryPlayedNotification;
  protected readonly cottagePlayedNotification = cottagePlayedNotification;
  protected readonly guardhousePlayedNotification = guardhousePlayedNotification;
  protected readonly barracksPlayedNotification = barracksPlayedNotification;
  protected readonly innPlayedNotification = innPlayedNotification;
  protected readonly castlePlayedNotification = castlePlayedNotification;
  protected readonly cardTakenNotification = cardTakenNotification;
  protected readonly meeplesSoldNotification = meeplesSoldNotification;
  protected readonly finalScoringNotification = finalScoringNotification;
  protected readonly gameEndNotification = gameEndNotification;
}
