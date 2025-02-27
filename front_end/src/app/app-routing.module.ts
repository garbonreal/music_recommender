import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RecommandComponent } from './recommand/recommand.component';

const routes: Routes = [
  {
    path:'recommand',
    component:RecommandComponent
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
