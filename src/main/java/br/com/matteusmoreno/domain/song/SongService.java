package br.com.matteusmoreno.domain.song;

import br.com.matteusmoreno.domain.artist.Artist;
import br.com.matteusmoreno.domain.song.request.CreateSongRequest;
import br.com.matteusmoreno.domain.song.request.UpdateSongRequest;
import br.com.matteusmoreno.exception.SongConflictException;
import br.com.matteusmoreno.exception.SongNotFoundException;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SongService {

    public Song createOrFindSong(CreateSongRequest request) {
        String normalizedTitle = request.title().trim().toUpperCase();
        String normalizedArtist = request.artistName().trim().toUpperCase();

        return findByTitleAndArtist(normalizedTitle, normalizedArtist).orElseGet(() -> {
            Song newSong = Song.builder()
                    .title(normalizedTitle)
                    .artistName(normalizedArtist)
                    .createdAt(LocalDateTime.now())
                    .build();
            newSong.persist();
            return newSong;
        });
    }

    public Song getSongById(ObjectId songId) {
        Song song = Song.findById(songId);
        if (song == null) throw new SongNotFoundException("Song not found with id: " + songId);

        return song;
    }

    public List<Song> getAllSongs(int page, int size) {
        return Song.findAll(Sort.ascending("title")).page(page, size).list();
    }

    public Song updateSong(UpdateSongRequest request) {
        Song song = getSongById(request.songId());

        String newTitle = request.title() != null ? request.title().trim().toUpperCase() : song.title;
        String newArtistName = request.artistName() != null ? request.artistName().trim().toUpperCase() : song.artistName;

        if (!newTitle.equals(song.title) || !newArtistName.equals(song.artistName)) {
            findByTitleAndArtist(newTitle, newArtistName).ifPresent(existingSong -> {
                throw new SongConflictException("Another song with the same title and artist already exists.");
            });
        }

        song.title = newTitle;
        song.artistName = newArtistName;

        song.update();
        return song;
    }

    public void deleteSong(ObjectId songId) {
        Song song = getSongById(songId);

        List<Artist> artistsToUpdate = Artist.list("repertoire", songId);

        for (Artist artist : artistsToUpdate) {
            artist.repertoire.remove(songId);
            artist.update();
        }

        song.delete();
    }

    // PRIVATE METHODS
    private Optional<Song> findByTitleAndArtist(String title, String artistName) {
        return Song.find(
                "title = :title and artistName = :artistName",
                Parameters.with("title", title).and("artistName", artistName)
        ).firstResultOptional();
    }
}