package yanagishima.controller;

import static yanagishima.util.DownloadUtil.downloadCsv;
import static yanagishima.util.DownloadUtil.downloadTsv;
import static yanagishima.util.HistoryUtil.createHistoryResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import yanagishima.config.YanagishimaConfig;
import yanagishima.model.db.Comment;
import yanagishima.model.db.Query;
import yanagishima.model.db.SessionProperty;
import yanagishima.service.CommentService;
import yanagishima.service.PublishService;
import yanagishima.service.QueryService;
import yanagishima.service.SessionPropertyService;

@Slf4j
@Api(tags = "download")
@RestController
@RequiredArgsConstructor
public class ShareController {
  private final PublishService publishService;
  private final QueryService queryService;
  private final SessionPropertyService sessionPropertyService;
  private final CommentService commentService;
  private final YanagishimaConfig config;

  @GetMapping("share/csvdownload")
  public void get(@RequestParam(name = "publish_id", required = false) String publishId,
                  @RequestParam(defaultValue = "UTF-8") String encode,
                  @RequestParam(defaultValue = "true") boolean header,
                  @RequestParam(defaultValue = "true") boolean bom,
                  HttpServletResponse response) {
    if (publishId == null) {
      return;
    }

    publishService.get(publishId).ifPresent(publish -> {
      String fileName = publishId + ".csv";
      downloadCsv(response, fileName, publish.getDatasource(), publish.getQueryId(), encode, header, bom);
    });
  }

  @GetMapping("share/download")
  public void download(@RequestParam(name = "publish_id", required = false) String publishId,
                       @RequestParam(defaultValue = "UTF-8") String encode,
                       @RequestParam(defaultValue = "true") boolean header,
                       @RequestParam(defaultValue = "true") boolean bom,
                       HttpServletResponse response) {
    if (publishId == null) {
      return;
    }

    publishService.get(publishId).ifPresent(publish -> {
      String fileName = publishId + ".tsv";
      downloadTsv(response, fileName, publish.getDatasource(), publish.getQueryId(), encode, header, bom);
    });
  }

  @GetMapping("share/shareHistory")
  public Map<String, Object> get(@RequestParam(name = "publish_id") String publishId) {
    Map<String, Object> body = new HashMap<>();
    try {
      publishService.get(publishId).ifPresent(publish -> {
        String datasource = publish.getDatasource();
        body.put("datasource", datasource);
        String queryId = publish.getQueryId();
        body.put("queryid", queryId);
        Query query = queryService.get(queryId, datasource).get();
        body.put("engine", query.getEngine());
        List<SessionProperty> sessionPropertyList = sessionPropertyService.getAll(datasource, query.getEngine(),
                                                                                  queryId);
        createHistoryResult(body, config.getSelectLimit(), datasource, query, true, sessionPropertyList);
        Optional<Comment> comment = commentService.get(datasource, query.getEngine(), queryId);
        body.put("comment", comment.orElse(null));
      });
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
      body.put("error", e.getMessage());
    }
    return body;
  }
}
