package yuhan.hgcq.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yuhan.hgcq.server.domain.Album;
import yuhan.hgcq.server.domain.Member;
import yuhan.hgcq.server.domain.Photo;
import yuhan.hgcq.server.dto.member.MemberDTO;
import yuhan.hgcq.server.dto.photo.AutoSavePhotoForm;
import yuhan.hgcq.server.dto.photo.MovePhotoForm;
import yuhan.hgcq.server.dto.photo.PhotoDTO;
import yuhan.hgcq.server.dto.photo.UploadPhotoForm;
import yuhan.hgcq.server.service.AlbumService;
import yuhan.hgcq.server.service.MemberService;
import yuhan.hgcq.server.service.PhotoService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/photo")
public class PhotoController {

    private final MemberService ms;
    private final AlbumService as;
    private final PhotoService ps;

    /**
     * 사진 업로드
     *
     * @param form    사진 업로드 폼
     * @param request 요청
     * @return 성공 시 201 상태 코드, 업로드 실패 시 500번 상태 코드, 실패 시 401 상태 코드
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadPhotos(@ModelAttribute UploadPhotoForm form, HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            MemberDTO loginMember = (MemberDTO) session.getAttribute("member");

            if (loginMember != null) {
                Member findMember = ms.search(loginMember.getMemberId());

                if (findMember != null) {
                    Album fa = as.search(form.getAlbumId());

                    if (fa != null) {
                        try {
                            ps.save(form);
                        } catch (IOException e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Photo Uploading Error");
                        }
                    }
                }
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not Login");
    }

    /**
     * 사진 삭제
     *
     * @param photoDTO 사진 DTO
     * @param request  요청
     * @return 성공 시 200 상태 코드, 실패 시 401 상태 코드
     */
    @PostMapping("/delete")
    public ResponseEntity<?> deletePhoto(@RequestBody PhotoDTO photoDTO, HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            MemberDTO loginMember = (MemberDTO) session.getAttribute("member");

            if (loginMember != null) {
                Member findMember = ms.search(loginMember.getMemberId());

                if (findMember != null) {
                    Photo fp = ps.search(photoDTO.getPhotoId());

                    if (fp != null) {
                        ps.delete(fp);

                        return ResponseEntity.status(HttpStatus.OK).build();
                    }
                }
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not Login");
    }

    /**
     * 사진 삭제 취소
     *
     * @param photoDTO 사진 DTO
     * @param request  요청
     * @return 성공 시 200 상태 코드, 실패 시 401 상태 코드
     */
    @PostMapping("/delete/cancel")
    public ResponseEntity<?> cancelDeletePhoto(@RequestBody PhotoDTO photoDTO, HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            MemberDTO loginMember = (MemberDTO) session.getAttribute("member");

            if (loginMember != null) {
                Member findMember = ms.search(loginMember.getMemberId());

                if (findMember != null) {
                    Photo fp = ps.search(photoDTO.getPhotoId());

                    if (fp != null) {
                        ps.deleteCancel(fp);

                        return ResponseEntity.status(HttpStatus.OK).build();
                    }
                }
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not Login");
    }

    /**
     * 앨범 이동
     *
     * @param form    앨범 이동 폼
     * @param request 요청
     * @return 성공 시 200 상태 코드, 실패 시 401 상태 코드
     */
    @PostMapping("/move")
    public ResponseEntity<?> movePhoto(@RequestBody MovePhotoForm form, HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            MemberDTO loginMember = (MemberDTO) session.getAttribute("member");

            if (loginMember != null) {
                Member findMember = ms.search(loginMember.getMemberId());

                if (findMember != null) {
                    Album fa = as.search(form.getNewAlbumId());

                    if (fa != null) {
                        List<PhotoDTO> photos = form.getPhotos();
                        List<Photo> photoList = new ArrayList<>();

                        for (PhotoDTO photoDTO : photos) {
                            Photo fp = ps.search(photoDTO.getPhotoId());
                            photoList.add(fp);
                        }

                        ps.move(fa, photoList);

                        return ResponseEntity.status(HttpStatus.OK).build();
                    }
                }
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not Login");
    }

    /**
     * 사진 앨범에 자동 저장
     *
     * @param form    사진 자동 저장 폼
     * @param request 요청
     * @return 성공 시 201 상태 코드, 업로드 실패 시 500번 상태 코드, 실패 시 401 상태 코드
     */
    @PostMapping("/autosave")
    public ResponseEntity<?> autosavePhoto(@RequestBody AutoSavePhotoForm form, HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            MemberDTO loginMember = (MemberDTO) session.getAttribute("member");

            if (loginMember != null) {
                Member findMember = ms.search(loginMember.getMemberId());

                if (findMember != null) {
                    try {
                        ps.autoSave(form);
                    } catch (IOException e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Photo Autosaving Error");
                    }
                }
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not Login");
    }

    /**
     * 사진 리스트
     *
     * @param albumId 앨범 id
     * @param request 요청
     * @return 성공 시 200 상태 코드와 사진 리스트, 실패 시 401 상태 코드
     */
    @GetMapping("/list/albumId")
    public ResponseEntity<?> listPhoto(@RequestParam("albumId") Long albumId, HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            MemberDTO loginMember = (MemberDTO) session.getAttribute("member");

            if (loginMember != null) {
                Member findMember = ms.search(loginMember.getMemberId());

                if (findMember != null) {
                    Album fa = as.search(albumId);

                    if (fa != null) {
                        List<Photo> photoList = ps.searchAll(fa);
                        List<PhotoDTO> photoDTOList = new ArrayList<>();

                        for (Photo photo : photoList) {
                            PhotoDTO dto = mapping(photo);
                            photoDTOList.add(dto);
                        }

                        return ResponseEntity.status(HttpStatus.OK).body(photoDTOList);
                    }
                }
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not Login");
    }

    /**
     * 휴지통 리스트
     *
     * @param albumId 앨범 id
     * @param request 요청
     * @return 성공 시 200 상태 코드와 사진 리스트, 실패 시 401 상태 코드
     */
    @GetMapping("/list/albumId/trash")
    public ResponseEntity<?> listPhotoTrash(@RequestParam("albumId") Long albumId, HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            MemberDTO loginMember = (MemberDTO) session.getAttribute("member");

            if (loginMember != null) {
                Member findMember = ms.search(loginMember.getMemberId());

                if (findMember != null) {
                    Album fa = as.search(albumId);

                    if (fa != null) {
                        List<Photo> trashList = ps.trashList(fa);
                        ps.trash(trashList);

                        List<Photo> trashListAfterClear = ps.trashList(fa);
                        List<PhotoDTO> photoDTOList = new ArrayList<>();

                        for (Photo photo : trashListAfterClear) {
                            PhotoDTO dto = mapping(photo);
                            photoDTOList.add(dto);
                        }

                        return ResponseEntity.status(HttpStatus.OK).body(photoDTOList);
                    }
                }
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not Login");
    }

    /**
     * Photo -> PhotoDTO
     *
     * @param photo 사진
     * @return PhotoDTO
     */
    private PhotoDTO mapping(Photo photo) {
        PhotoDTO dto = new PhotoDTO();
        dto.setPhotoId(photo.getId());
        dto.setAlbumId(photo.getAlbum().getId());
        dto.setCreated(photo.getCreated());
        dto.setName(photo.getName());
        dto.setPath(photo.getPath());
        return dto;
    }
}
