import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { interval, Subscription } from 'rxjs';


@Component({
  selector: 'recommand',
  templateUrl: './recommand.component.html',
  styleUrls: ['./recommand.component.css']
})
export class RecommandComponent implements OnInit {
  historyMusicList: any[] = [];
  recommandMusicList: any[] = [];
  popularMusicList: any[] = [];
  selectedUid: number | null = null;
  uidRange: number[] = Array.from({ length: 2100 }, (_, i) => i + 1);
  isCalculating: boolean = false;
  pollingSubscription: Subscription | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {}

  fetchMusicRatings(): void {
    if (this.selectedUid) {
      const url = `http://localhost:8080/rest/music?uid=${this.selectedUid}`;
      this.http.get<any>(url).subscribe(
        (data) => {
          this.historyMusicList = data.history_music || [];
          this.recommandMusicList = data.recommand_music || [];
          this.popularMusicList = data.popular_music || [];
        },
        (error) => {
          console.error('Error fetching data:', error);
        }
      );
    }
  }

  onArtistClick(musicId: number) {
    const apiUrl = `http://localhost:8080/rest/click?uid=${this.selectedUid}&mid=${musicId}`;
    this.http.get(apiUrl).subscribe(response => {
      console.log('Music clicked:', response);
      this.isCalculating = true;
      this.startPolling();
    });
  }

  startPolling(): void {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
    }

    this.pollingSubscription = interval(2000).subscribe(() => {
      if (this.selectedUid) {
        const statusUrl = `http://localhost:8080/rest/status?uid=${this.selectedUid}`;
        this.http.get<any>(statusUrl).subscribe(
          (statusData) => {
            console.log('Status response:', statusData);

            if (statusData.status === "done") {
              this.fetchMusicRatings();          
              this.stopPolling();
              this.isCalculating = false;
            }
          },
          (error) => {
            console.error('Error checking status:', error);
          }
        );
      }
    });
  }

  stopPolling(): void {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
      this.pollingSubscription = null;
      this.isCalculating = false;
    }
  }
}

