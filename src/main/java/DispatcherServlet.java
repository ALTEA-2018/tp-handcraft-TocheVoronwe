import annotations.Controller;
import annotations.RequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@WebServlet(urlPatterns = "/*", loadOnStartup = 1)
public class DispatcherServlet extends HttpServlet {

    private Map<String, Method> uriMappings = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        System.out.println("Getting request for " + req.getRequestURI());

        var method = this.getMappingForUri(req.getRequestURI());
        if (method == null) {
            resp.sendError(404, "no mapping found for request uri " + req.getRequestURI());
            return;
        }
        try {
            var invokeClass = method.getDeclaringClass().newInstance();
            var result = new Object();
            if (req.getParameterMap().isEmpty())
                result = method.invoke(invokeClass);
            else
                result = method.invoke(invokeClass, req.getParameterMap());
            resp.getWriter().print(result.toString());
            System.out.println("PRINTING RESULT");
        }
        catch (InvocationTargetException ie) {
            resp.sendError(500, ie.getMessage());
        } catch (IllegalAccessException iae) {
            resp.sendError(500, iae.getLocalizedMessage());
        } catch (InstantiationException iie) {
            resp.sendError(500, iie.getLocalizedMessage());
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // on enregistre notre controller au démarrage de la servlet
        this.registerController(HelloController.class);
    }

    protected void registerController(Class controllerClass) {
        System.out.println("Analysing class " + controllerClass.getName());
        var res = controllerClass.getAnnotation(Controller.class);
        if (res == null || res.annotationType() != Controller.class)
            throw new IllegalArgumentException();
        for (Method method : controllerClass.getMethods()) {
            if (method.getReturnType() != Void.TYPE)
                registerMethod(method);
        }
    }

    protected void registerMethod(Method method) {
        System.out.println("Registering method " + method.getName());
        var res = method.getAnnotation(RequestMapping.class);
        if (res == null
                || !res.annotationType().equals(RequestMapping.class)
                || res.uri().isEmpty()) {
            return;
        }
        this.uriMappings.put(res.uri(), method);
    }

    protected Map<String, Method> getMappings() {
        return this.uriMappings;
    }

    protected Method getMappingForUri(String uri) {
        return this.uriMappings.get(uri);
    }
}
