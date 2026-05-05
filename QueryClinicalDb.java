import java.sql.*;
public class QueryClinicalDb {
  public static void main(String[] args) throws Exception {
    Class.forName("org.postgresql.Driver");
    String url = "jdbc:postgresql://ep-cool-breeze-agqhhjlc.c-2.eu-central-1.aws.neon.tech:5432/nephros_clinical_service.db?sslmode=require&channel_binding=require";
    try (Connection c = DriverManager.getConnection(url, "neondb_owner", "npg_Zb1Kda5LPlin")) {
      String sql = "select lr.id as lab_request_id, lr.consultation_id, lr.status, res.id as lab_result_id, res.file_name, res.file_path, res.uploaded_at from lab_requests lr left join lateral (select * from lab_results r where r.lab_request_id = lr.id order by r.uploaded_at desc limit 1) res on true where lr.consultation_id = ?::uuid order by lr.created_at desc";
      try (PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, "3661716b-faa8-4b85-9ad0-cb97fb9223b3");
        try (ResultSet rs = ps.executeQuery()) {
          ResultSetMetaData md = rs.getMetaData();
          int cols = md.getColumnCount();
          while (rs.next()) {
            for (int i = 1; i <= cols; i++) {
              System.out.println(md.getColumnLabel(i) + "=" + rs.getString(i));
            }
            System.out.println("---");
          }
        }
      }
    }
  }
}
