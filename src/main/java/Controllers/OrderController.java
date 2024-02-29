package Controllers;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.*;
import Models.*;
import DAO.*;

@WebServlet("/OrderController")
public class OrderController extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	RequestDispatcher dispatcher = null;
	OrderDAO orderDAO = null;
	AddressDAO addressDAO = null;
    HistoryDAO historyDAO = null;   
    NoteDAO noteDAO = null;
	
    public OrderController() throws ClassNotFoundException, SQLException {
        super();
        orderDAO = new OrderDAO();
        addressDAO = new AddressDAO();
        historyDAO = new HistoryDAO();
        noteDAO = new NoteDAO();
    }


	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String page = request.getParameter("page");
		if(page != null) {
			switch(page) {
			
			case "detail":
				detailOfOrder(request, response);
				break;
				
			case "detailOfHistory":
				detailOfOrderCompleted(request, response);
				break;
				
			case "orderCancel":
				orderCancel(request, response);
				break;
				
			case "delete":
				deleteOrder(request, response);
				break;
				
			case "transfer":
				transferToAdmin(request, response);
				break;
				
			case "shipOrder":
				shipOrder(request, response);
				break;
			
			}
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}
	
	// ship the ordre
	private void shipOrder(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String order_code = request.getParameter("order_code");
		String product_id = request.getParameter("product_id");
		String filter_value = request.getParameter("filter_value");
		String admin_id = request.getParameter("admin_id");
		
		System.out.println(filter_value);
		
		boolean flag = orderDAO.shipOrder(order_code, Integer.parseInt(product_id));
		if(flag) {
			System.out.println("updated product status");
			// get the order item
			Orders order = orderDAO.getByCodeAndProductID(order_code, Integer.parseInt(product_id));
			History history = new History();
			history.setCount(order.getCount());
			history.setCustomer_id(order.getCustomer_id());
			history.setOrder_code(order.getOrder_code());
			history.setPrice(order.getPrice());
			history.setProduct_id(order.getProduct_id());
			history.setSeller_id(order.getSeller_id());
			history.setShipping_id(order.getShipping_id());
			boolean inserted_history = historyDAO.create(history);
			if(inserted_history) {
				System.out.println("updated to history ");
	        	String success = "Shipped to the Customer!";
				String encoded = URLEncoder.encode(success, "UTF-8");
				response.sendRedirect(request.getContextPath() + "/AdminController?page=dashboard&admin_id="+admin_id+"&filter_value="+filter_value+"&success="+encoded);
			}
		}
		
	}
	
	// order canceling
	private void orderCancel(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String user_id = request.getParameter("user_id");
		String filter_value = request.getParameter("filter_value");
		String order_code = request.getParameter("order_code");
		String product_id = request.getParameter("product_id");
		
		boolean flag = orderDAO.orderCancelByUser(order_code, Integer.parseInt(product_id));
		if(flag) {
			String success = "You canceled the order";
			String encoded = URLEncoder.encode(success, "UTF-8");
			response.sendRedirect(request.getContextPath() + "/UserController?page=order&user_id="+user_id+
					"&filter_value="+filter_value+"&success="+success);
		}else {
			String error = "You can't canceled the order";
			String encoded = URLEncoder.encode(error, "UTF-8");
			response.sendRedirect(request.getContextPath() + "/UserController?page=order&user_id="+user_id+
					"&filter_value="+filter_value+"&error="+error);
		}
	}
	
	// order detail
	private void detailOfOrder(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String seller_id = request.getParameter("seller_id");
		String order_code = request.getParameter("order_code");
		
		List<Orders> orders = orderDAO.getByOrderCodeWithPending(order_code, Integer.parseInt(seller_id)); // get the order code by seller id where the status 0
		
		System.out.println(orders);
		// Get the first order in the list
	    Orders firstOrder = orders.get(0);
	    // Get the shipping ID of the first order
	    int shipping_id = firstOrder.getShipping_id();
	    // Retrieve the address associated with the shipping ID
	    Address address = addressDAO.getById(shipping_id);
	    
	    double total = 0.0;
	    for(Orders o : orders) {
	    	double temp = o.getPrice() * o.getCount();
	    	total += temp;
	    }
	    
	    // get the note
	    Note note = noteDAO.getByOrderCode(order_code);
		
	    request.setAttribute("note", note);
		request.setAttribute("address", address);
		request.setAttribute("orders", orders);
		request.setAttribute("total", total);
		
		dispatcher = request.getRequestDispatcher("/views/seller/order/detail.jsp");
		dispatcher.forward(request, response);
	}
	
	private void detailOfOrderCompleted(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String seller_id = request.getParameter("seller_id");
		String order_code = request.getParameter("order_code");
		
		List<Orders> orders = orderDAO.getByOrderCodeWithComplete(order_code, Integer.parseInt(seller_id)); // get the order code by seller id where the status 0
		
		System.out.println(orders);
		// Get the first order in the list
	    Orders firstOrder = orders.get(0);
	    // Get the shipping ID of the first order
	    int shipping_id = firstOrder.getShipping_id();
	    // Retrieve the address associated with the shipping ID
	    Address address = addressDAO.getById(shipping_id);
	    
	    double total = 0.0;
	    for(Orders o : orders) {
	    	double temp = o.getPrice() * o.getCount();
	    	total += temp;
	    }
		
		request.setAttribute("address", address);
		request.setAttribute("orders", orders);
		request.setAttribute("total", total);
		
		dispatcher = request.getRequestDispatcher("/views/seller/order/historyOfDetail.jsp");
		dispatcher.forward(request, response);
	}

	// delete order from seller
	private void deleteOrder(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String seller_id = request.getParameter("seller_id");
		String product_id = request.getParameter("product_id");
		String order_code = request.getParameter("order_code");
		
		boolean flag = orderDAO.callOffOrder(Integer.parseInt(product_id), order_code);
		if(flag) {
			String success = "You've removed order Successfully";
			String encoded = URLEncoder.encode(success, "UTF-8");
    		response.sendRedirect(request.getContextPath() + "/SellerController?page=order&success="+encoded+"&seller_id="+seller_id);
		}else {
			String success = "You've can't remove order.";
			String encoded = URLEncoder.encode(success, "UTF-8");
    		response.sendRedirect(request.getContextPath() + "/SellerController?page=order&error="+encoded+"&seller_id="+seller_id);
		}
	}

	// transfer to admin
	private void transferToAdmin(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String seller_id = request.getParameter("seller_id");
		String order_code = request.getParameter("order_code");
		String product_id = request.getParameter("product_id");
		
		// update the status to 1
		boolean flag = orderDAO.transferOrder(Integer.parseInt(product_id), order_code);
		
		if(flag) {
			String success = "Successfully transfered to warehouse.";
			String encoded = URLEncoder.encode(success, "UTF-8");
    		response.sendRedirect(request.getContextPath() + "/SellerController?page=order&success="+encoded+"&seller_id="+seller_id);
		}else {
			String success = "Can't transfer to warehouse.";
			String encoded = URLEncoder.encode(success, "UTF-8");
    		response.sendRedirect(request.getContextPath() + "/SellerController?page=order&error="+encoded+"&seller_id="+seller_id);
		}
	}

}
