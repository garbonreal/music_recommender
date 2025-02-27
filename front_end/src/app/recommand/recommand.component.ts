import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';


@Component({
  selector: 'recommand',
  templateUrl: './recommand.component.html',
  styleUrls: ['./recommand.component.css']
})
export class RecommandComponent implements OnInit {
  musicList: any[] = [];
  selectedUid: number | null = null;
  uidRange: number[] = Array.from({ length: 2100 }, (_, i) => i + 1);

  constructor(private http: HttpClient) {}

  ngOnInit(): void {}

  fetchMusicRatings(): void {
    if (this.selectedUid) {
      const url = `http://localhost:8080/rest/music/recommand?uid=${this.selectedUid}`;
      this.http.get<any>(url).subscribe(
        (data) => {
          this.musicList = data.music;
        },
        (error) => {
          console.error('Error fetching data:', error);
        }
      );
    }
  }

  onArtistClick(musicId: number) {
    const apiUrl = `http://localhost:8080/rest/music/click?uid=${this.selectedUid}&mid=${musicId}`;
    this.http.get(apiUrl).subscribe(response => {
      console.log('Music clicked:', response);
    });
  }
}

