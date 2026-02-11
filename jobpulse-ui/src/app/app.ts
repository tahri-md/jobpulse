import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from './components/toast/toast.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastComponent],
  template: `
    <app-toast />
    <router-outlet />
  `,
  styles: [`:host { display: block; }`]
})
export class App {}
