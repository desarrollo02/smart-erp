package py.com.logixone.web.shell;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "RootEntryServlet", urlPatterns = "")
public final class RootEntryServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String workspace = request.getContextPath() + "/faces/app/index.xhtml";
        response.sendRedirect(response.encodeRedirectURL(workspace));
    }
}
