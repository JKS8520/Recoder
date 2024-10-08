package yuhan.hgcq.server.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yuhan.hgcq.server.domain.Album;
import yuhan.hgcq.server.domain.Photo;
import yuhan.hgcq.server.domain.Team;
import yuhan.hgcq.server.dto.photo.AutoSavePhotoForm;
import yuhan.hgcq.server.dto.photo.UploadPhotoForm;
import yuhan.hgcq.server.repository.AlbumRepository;
import yuhan.hgcq.server.repository.LikedRepository;
import yuhan.hgcq.server.repository.PhotoRepository;
import yuhan.hgcq.server.repository.TeamRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PhotoService {
    private static final Logger log = LoggerFactory.getLogger(PhotoService.class);

    private final PhotoRepository pr;
    private final AlbumRepository ar;
    private final TeamRepository tr;
    private final LikedRepository lr;

    private final static int DELETE_DAY = 30;
    private final static String DIRECTORY_PATH = File.separator
            + "app" + File.separator
            + "images" + File.separator;

    /**
     * Upload photo
     *
     * @param photo photo
     * @return photoId
     * @throws IllegalArgumentException Argument is wrong
     */
    @Transactional
    public Long savePhoto(Photo photo) throws IllegalArgumentException {
        ensureNotNull(photo, "Photo");

        Long saveId = pr.save(photo);
        log.info("Save Photo : {}", photo);
        return saveId;
    }

    @Transactional
    public void savePhoto(Album album, String path, String region, String create) throws IOException {
        Path tempPath = Paths.get(path);
        Long albumId = album.getId();

        if (!Files.exists(tempPath)) {
            throw new IOException("Temp file does not exist");
        }

        String dirPath = DIRECTORY_PATH + albumId + File.separator;
        String name = tempPath.getFileName().toString();
        Path newPath = Paths.get(dirPath + name);

        Files.move(tempPath, newPath, StandardCopyOption.REPLACE_EXISTING);

        String imagePath = "/images/" + albumId + "/" + name;
        Photo p = new Photo(album, name, imagePath, region, LocalDateTime.parse(create));

        pr.save(p);
        log.info("Save Photo : {}", p);
    }

    /**
     * Upload photoList
     *
     * @param form photoList
     * @throws IOException              Upload error
     * @throws IllegalArgumentException Argument is wrong
     */
    @Transactional
    public void savePhoto(UploadPhotoForm form) throws IOException, IllegalArgumentException {
        List<MultipartFile> files = form.getFiles();
        List<String> creates = form.getCreates();
        List<String> regions = form.getRegions();

        log.info("files size : {}", files.size());
        log.info("creates size : {}", creates.size());
        log.info("regions size : {}", regions.size());


        ensureNotNull(files, "Files");
        ensureNotNull(creates, "Creates");

        int size = files.size();
        Long albumId = form.getAlbumId();
        Album fa = ar.findOne(albumId);
        List<String> nameList = pr.findNameAll(fa);

        try {
            String newPath = DIRECTORY_PATH + albumId + File.separator;
            File directory = new File(newPath);

            if (!directory.exists()) {
                directory.mkdirs();
            }

            for (int i = 0; i < size; i++) {
                MultipartFile file = files.get(i);
                String region = regions.get(i);
                String create = creates.get(i);
                String name = file.getOriginalFilename();

                if (nameList.contains(name)) {
                    continue;
                }

                Path path = Paths.get(newPath + name);
                file.transferTo(path);
                String imagePath = "/images/" + albumId + "/" + name;

                Photo p = new Photo(fa, name, imagePath, region, LocalDateTime.parse(create));
                pr.save(p);

                log.info("Save Photos : {}", p);
            }
        } catch (IOException e) {
            log.error("Upload Photo Error");
            throw new IOException();
        }
    }

    /**
     * Delete photo
     *
     * @param photo photo
     * @throws IllegalArgumentException Argument is wrong
     */
    @Transactional
    public void deletePhoto(Photo photo) throws IllegalArgumentException {
        ensureNotNull(photo, "Photo");

        photo.delete();

        pr.save(photo);
        lr.delete(photo);
        log.info("Delete Photo : {}", photo);
    }

    /**
     * Delete photo cancel
     *
     * @param photo photo
     * @throws IllegalArgumentException Argument is wrong
     */
    @Transactional
    public void deleteCancelPhoto(Photo photo) throws IllegalArgumentException {
        ensureNotNull(photo, "Photo");

        photo.cancelDelete();

        pr.save(photo);
        log.info("Delete Cancel Photo : {}", photo);
    }

    /**
     * Trash empty
     *
     * @param photos photoTrashList
     */
    @Transactional
    public void trash(List<Photo> photos) {
        LocalDateTime now = LocalDateTime.now();
        for (Photo photo : photos) {
            LocalDateTime deleted = photo.getDeleted();
            long between = ChronoUnit.DAYS.between(deleted, now);

            if (between >= DELETE_DAY) {
                pr.delete(photo.getId());
                log.info("Complete Delete Photo : {}", photo);
            }
        }
    }

    /**
     * Find photo by path
     *
     * @param id photoId
     * @return photo
     * @throws IllegalArgumentException Argument is wrong
     */
    public Photo searchOne(Long id) throws IllegalArgumentException {
        Photo find = pr.findOne(id);

        if (find == null) {
            throw new IllegalArgumentException("Photo not found");
        }

        return find;
    }

    /**
     * Find photo by path
     *
     * @param path path
     * @return photo
     * @throws IllegalArgumentException Argument is wrong
     */
    public Photo searchOne(String path) throws IllegalArgumentException {
        Photo find = pr.findByPath(path);

        if (find == null) {
            throw new IllegalArgumentException("Photo not found");
        }

        return find;
    }

    /**
     * Find photoList
     *
     * @param album album
     * @return photoList
     * @throws IllegalArgumentException Argument is wrong
     */
    public List<Photo> searchAll(Album album) throws IllegalArgumentException {
        ensureNotNull(album, "Album");

        return pr.findAll(album);
    }

    /**
     * Find photoTrashList
     *
     * @param album album
     * @return photoTrashList
     * @throws IllegalArgumentException Argument is wrong
     */
    public List<Photo> searchTrashList(Album album) throws IllegalArgumentException {
        ensureNotNull(album, "Album");

        return pr.findByDeleted(album);
    }

    /**
     * Auto save photoList
     *
     * @param form photoList
     * @throws IOException Upload error
     */
    @Transactional
    public void autoSave(AutoSavePhotoForm form) throws IOException {
        Long teamId = form.getTeamId();
        Team ft = tr.findOne(teamId);

        Set<String> albumNames = ar.findAlbumName(ft);
        List<MultipartFile> files = form.getFiles();
        List<String> creates = form.getCreates();
        List<String> regions = form.getRegions();

        int size = files.size();

        for (int i = 0; i < size; i++) {
            String region = regions.get(i);
            Album fa = null;

            if (region.equals("null")) {
                if (albumNames.contains("위치 정보 없음")) {
                    fa = ar.findOneByName(ft, "위치 정보 없음");
                } else {
                    Album album = new Album(ft, "위치 정보 없음");
                    Long saveId = ar.save(album);
                    log.info("Save Album : {}", album);
                    fa = ar.findOne(saveId);
                    albumNames.add(fa.getName());
                }
            } else if (albumNames.contains(region)) {
                fa = ar.findOneByName(ft, region);
            } else {
                Album album = new Album(ft, region);
                Long saveId = ar.save(album);
                log.info("Save Album : {}", album);
                fa = ar.findOne(saveId);
                albumNames.add(fa.getName());
            }

            if (fa != null) {
                try {
                    List<String> nameList = pr.findNameAll(fa);
                    Long albumId = fa.getId();

                    String newPath = DIRECTORY_PATH + albumId + File.separator;
                    File directory = new File(newPath);

                    if (!directory.exists()) {
                        directory.mkdirs();
                    }

                    MultipartFile file = files.get(i);
                    String name = file.getOriginalFilename();

                    if (nameList.contains(name)) {
                        continue;
                    }

                    Path path = Paths.get(newPath + name);
                    file.transferTo(path);
                    String imagePath = "/images/" + albumId + "/" + name;

                    Photo p = new Photo(fa, name, imagePath, region, LocalDateTime.parse(creates.get(i)));
                    pr.save(p);

                    log.info("AutoSave Photo : {}", p);
                } catch (IOException e) {
                    log.error("AutoSave Photo Error");
                    throw new IOException();
                }
            }
        }
    }

    @Transactional
    public void autoSave(Team team, String path, String region, String create) throws IOException {
        Set<String> albumNames = ar.findAlbumName(team);

        Album fa = null;

        if (region.equals("null")) {
            if (albumNames.contains("위치 정보 없음")) {
                fa = ar.findOneByName(team, "위치 정보 없음");
            } else {
                Album album = new Album(team, "위치 정보 없음");
                Long saveId = ar.save(album);
                log.info("Save Album : {}", album);
                fa = ar.findOne(saveId);
                albumNames.add(fa.getName());
            }
        } else if (albumNames.contains(region)) {
            fa = ar.findOneByName(team, region);
        } else {
            Album album = new Album(team, region);
            Long saveId = ar.save(album);
            log.info("Save Album : {}", album);
            fa = ar.findOne(saveId);
            albumNames.add(fa.getName());
        }

        if (fa != null) {
            try {
                List<String> nameList = pr.findNameAll(fa);
                Path tempPath = Paths.get(path);
                Long albumId = fa.getId();

                if (!Files.exists(tempPath)) {
                    throw new IOException("Temp file does not exist");
                }

                String dirPath = DIRECTORY_PATH + albumId + File.separator;
                String name = tempPath.getFileName().toString();
                Path newPath = Paths.get(dirPath + name);

                File dir = new File(dirPath);

                if (!dir.exists()) {
                    dir.mkdirs();
                }

                if (nameList.contains(name)) {
                    return;
                }

                Files.move(tempPath, newPath, StandardCopyOption.REPLACE_EXISTING);

                String imagePath = "/images/" + albumId + "/" + name;
                Photo p = new Photo(fa, name, imagePath, region, LocalDateTime.parse(create));

                pr.save(p);
            } catch (IOException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    /**
     * Move photo to album
     *
     * @param newAlbum new album
     * @param photos   photoList
     * @throws IllegalArgumentException Argument is wrong
     */
    @Transactional
    public void move(Album newAlbum, List<Photo> photos) throws IllegalArgumentException {
        ensureNotNull(newAlbum, "Album");
        ensureNotNull(photos, "Photos");

        for (Photo photo : photos) {
            photo.changeAlbum(newAlbum);
            pr.save(photo);
            log.info("Move Photo : {}", photo);
        }
    }

    /**
     * Argument Check if Null
     *
     * @param obj  argument
     * @param name by log
     */
    private void ensureNotNull(Object obj, String name) {
        if (obj == null) {
            throw new IllegalArgumentException(name + " is null");
        }
    }
}
