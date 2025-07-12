import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StationForm } from './station-form';

describe('StationForm', () => {
  let component: StationForm;
  let fixture: ComponentFixture<StationForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StationForm]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StationForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
