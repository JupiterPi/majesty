import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppComponent } from './app.component';
import {HttpClientModule} from "@angular/common/http";
import {RouterModule} from "@angular/router";
import { LobbyComponent } from './lobby/lobby.component';
import {FormsModule} from "@angular/forms";
import { GameComponent } from './game/game.component';
import {CookieModule} from "ngx-cookie";

@NgModule({
  declarations: [
    AppComponent,
    LobbyComponent,
    GameComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    RouterModule.forRoot([
      {path: "", component: AppComponent},
    ]),
    FormsModule,
    CookieModule.withOptions(),
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
