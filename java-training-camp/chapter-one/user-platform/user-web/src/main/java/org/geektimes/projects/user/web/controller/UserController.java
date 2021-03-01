package org.geektimes.projects.user.web.controller;

import org.geektimes.projects.user.domain.User;
import org.geektimes.projects.user.service.UserService;
import org.geektimes.projects.user.service.impl.UserServiceImpl;
import org.geektimes.web.mvc.controller.PageController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * @author tanheyuan
 * @version 1.0
 * @since 2021/3/1
 */
@Path("")
public class UserController implements PageController {

    private static final UserService userService;

    static {
        userService = new UserServiceImpl();
    }

    @GET
    @Path("/login")
    public String login(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        System.out.println("====进入登录/注册页面====");
        return "login-form.jsp";
    }

    @POST
    @Path("/register")
    public String register(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        System.out.println("====进行注册====");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);

        boolean resutl = userService.register(user);
        if (resutl) {
            return "success.jsp";
        }
        return "failed.jsp";
    }
}