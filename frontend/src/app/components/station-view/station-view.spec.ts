import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StationView } from './station-view';

describe('StationView', () => {
  let component: StationView;
  let fixture: ComponentFixture<StationView>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StationView]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StationView);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
