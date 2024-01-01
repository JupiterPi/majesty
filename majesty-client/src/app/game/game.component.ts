import {Component} from '@angular/core';
import {Card, CardInQueue, Game, Place, Player} from "../data";

@Component({
  selector: 'app-game',
  templateUrl: './game.component.html',
  styleUrls: ['./game.component.scss']
})
export class GameComponent {
  // mock data
  game: Game = {
    players: [
      {
        name: "Player 1",
        score: 10,
        meeples: 3,
        cards: new Map<Place, Card[]>([
          ["MILL", [{places: ["MILL"]}, {places: ["MILL"]}]],
          ["BREWERY", [{places: ["BREWERY"]}, {places: ["MILL", "BREWERY"]}]],
          ["COTTAGE", [{places: ["COTTAGE"]}]],
          ["GUARDHOUSE", [{places: ["GUARDHOUSE"]}, {places: ["GUARDHOUSE", "BARRACKS"]}, {places: ["GUARDHOUSE"]}]],
          ["BARRACKS", []],
          ["INN", [{places: ["INN"]}, {places: ["INN"]}]],
          ["CASTLE", [{places: ["CASTLE"]}, {places: ["CASTLE"]}, {places: ["CASTLE"]}]],
        ]),
        infirmary: [],
      },
      {
        name: "Player 2",
        score: 15,
        meeples: 3,
        cards: new Map<Place, Card[]>([
          ["MILL", [{places: ["MILL"]}]],
          ["BREWERY", [{places: ["BREWERY"]}, {places: ["MILL", "BREWERY"]}]],
          ["COTTAGE", [{places: ["COTTAGE"]}, {places: ["COTTAGE"]}]],
          ["GUARDHOUSE", [{places: ["GUARDHOUSE"]}, {places: ["GUARDHOUSE"]}]],
          ["BARRACKS", [{places: ["BARRACKS"]}, {places: ["BARRACKS"]}]],
          ["INN", [{places: ["INN"]}, {places: ["INN"]}]],
          ["CASTLE", [{places: ["CASTLE"]}, {places: ["CASTLE"]}]],
        ]),
        infirmary: [],
      },
      {
        name: "Player 3",
        score: 25,
        meeples: 4,
        cards: new Map<Place, Card[]>([
          ["MILL", [{places: ["MILL"]}]],
          ["BREWERY", [{places: ["BREWERY"]}, {places: ["MILL", "BREWERY"]}]],
          ["COTTAGE", [{places: ["COTTAGE"]}, {places: ["COTTAGE"]}]],
          ["GUARDHOUSE", [{places: ["GUARDHOUSE"]}, {places: ["GUARDHOUSE"]}]],
          ["BARRACKS", [{places: ["BARRACKS"]}, {places: ["BARRACKS"]}]],
          ["INN", [{places: ["INN"]}, {places: ["INN"]}]],
          ["CASTLE", [{places: ["CASTLE"]}, {places: ["CASTLE"]}]],
        ]),
        infirmary: [],
      },
    ],
    bSide: false,
    cardsQueue: [
      {
        card: {
          places: ["MILL"]
        },
        meeples: 2
      },
      {
        card: {
          places: ["BREWERY"]
        },
        meeples: 1
      },
      {
        card: {
          places: ["BARRACKS", "CASTLE"]
        },
        meeples: 1
      },
      {
        card: {
          places: ["COTTAGE"]
        },
        meeples: 0
      },
      {
        card: {
          places: ["GUARDHOUSE"]
        },
        meeples: 0
      },
      /*{
        card: {
          places: ["BARRACKS"]
        },
        meeples: 0
      },
      {
        card: {
          places: ["INN"]
        },
        meeples: 0
      },
      {
        card: {
          places: ["CASTLE"]
        },
        meeples: 0
      },*/
    ]
  };
  cards(player: Player) {
    return Array.from(player.cards.entries());
  }
  isLeadingPlayer(player: Player) {
    return player == this.game.players.slice().sort((a, b) => b.score - a.score)[0];
  }

  queueSelectable = true;
  selectCardInQueue(place: Place) {
    console.log("selected card in queue:", place);
  }
}
