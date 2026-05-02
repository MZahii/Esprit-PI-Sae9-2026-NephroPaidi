package tn.esprit.spring.apigateway.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SwaggerUiController {

    @GetMapping(value = "/swagger/{service}/index.html", produces = MediaType.TEXT_HTML_VALUE)
    public String swaggerUi(@PathVariable String service) {
        String openApiUrl = "/v3/api-docs/" + service;
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui.css" />
                  <title>%s - Swagger UI</title>
                </head>
                <body>
                  <div id="swagger-ui"></div>
                  <script src="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
                  <script>
                    window.ui = SwaggerUIBundle({
                      url: '%s',
                      dom_id: '#swagger-ui',
                      deepLinking: true,
                      displayOperationId: true,
                      defaultModelsExpandDepth: -1,
                      presets: [
                        SwaggerUIBundle.presets.apis,
                        SwaggerUIBundle.SwaggerUIStandalonePreset
                      ]
                    });
                  </script>
                </body>
                </html>
                """.formatted(service, openApiUrl);
    }

    @GetMapping("/swagger/{service}")
    public ResponseEntity<Void> swaggerUiRedirect(@PathVariable String service) {
      return ResponseEntity.status(302)
          .header("Location", "/swagger/%s/index.html".formatted(service))
          .build();
    }
}