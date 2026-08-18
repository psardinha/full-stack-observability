import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App', () => {
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render title', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Trace Initiator');
  });

  it('should allow reverse with an empty textbox', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const reverseButton = fixture.nativeElement.querySelectorAll('button')[0] as HTMLButtonElement;
    reverseButton.click();

    const request = httpTestingController.expectOne((request) => request.url.endsWith('/utils/reverse'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ input: ''});

    request.flush({output: ''});
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Result:');
  });

  it('should allow reverse to generate an exception', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    // Simulate user input of 'ERROR' in the input field
    const input = fixture.nativeElement.querySelector('#string-argument') as HTMLInputElement;
    input.value = 'ERROR';
    input.dispatchEvent(new Event('input'));

    const reverseButton = fixture.nativeElement.querySelectorAll('button')[0] as HTMLButtonElement;
    reverseButton.click();

    const request = httpTestingController.expectOne((request) => request.url.endsWith('/utils/reverse'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({input: 'ERROR'});

    request.flush({message: "Input cannot be 'ERROR'"}, {status: 400, statusText: 'Bad Request'});
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Reverse failed (HTTP 400, Input cannot be 'ERROR')");
  });

  it('should allow length with an empty textbox', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const lengthButton = fixture.nativeElement.querySelectorAll('button')[1] as HTMLButtonElement;
    lengthButton.click();

    const request = httpTestingController.expectOne(
      (request) => request.url.endsWith('/utils/length') && request.params.get('string') === '',
    );
    expect(request.request.method).toBe('GET');

    request.flush({ length: 0 });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Result: 0');
  });

  it('should mix reverse and length responses in chronological order', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const mixButton = fixture.nativeElement.querySelectorAll('button')[2] as HTMLButtonElement;
    mixButton.click();

    const requests = httpTestingController.match((request) => request.url.endsWith('/utils/reverse') || 
                                                              request.url.endsWith('/utils/length'));
    expect(requests.length).toBe(2);

    const reverseRequest = requests.find((request) => request.request.method === 'POST');
    const lengthRequest = requests.find((request) => request.request.method === 'GET');

    expect(reverseRequest).toBeDefined();
    expect(lengthRequest).toBeDefined();

    expect(reverseRequest!.request.body).toEqual({input: ''});

    // Flush length first to verify output follows completion order, not fixed operation order.
    lengthRequest!.flush({length: 3});
    reverseRequest!.flush({output: 'cba'});
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Result: String length: 3;Reverted string: cba');
  });
});
