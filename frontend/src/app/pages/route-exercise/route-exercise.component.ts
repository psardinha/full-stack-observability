import {Location} from '@angular/common';
import {Component, inject} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {toSignal} from '@angular/core/rxjs-interop';
import {map} from 'rxjs';

@Component({
  selector: 'app-route-exercise',
  standalone: true,
  template: `
    <section>
      <h1>Route exercise</h1>
      <p>Content of text box in previous page values: {{ previousValue() }}</p>
      <button type="button" (click)="goBack()">Back</button>
    </section>
  `
})

export class RouteExerciseComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);

  protected readonly previousValue = toSignal(this.route.queryParamMap.pipe(map(params => params.get('value') ?? '')),
                                              {initialValue: ''});

  protected goBack(): void {
    this.location.back();
  }
}
